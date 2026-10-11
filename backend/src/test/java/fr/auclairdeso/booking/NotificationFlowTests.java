package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.jayway.jsonpath.JsonPath;
import fr.auclairdeso.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Who receives which email, from the booking request to the evening program (docs/specification.md, 7).
 * The scheduling is off: the test calls the deadlines itself, after moving its clock.
 */
@SpringBootTest(properties = {
    "auclairdeso.identity.practitioners=so@example.com",
    "auclairdeso.notification.practitioner-phone=06 00 00 00 00",
    "auclairdeso.scheduling.enabled=false"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, NotificationFlowTests.TestClock.class})
@Testcontainers(disabledWithoutDocker = true)
class NotificationFlowTests {

    /** A clock the test sets where it needs, unlike the frozen clocks of the other tests. */
    static final class SettableClock extends Clock {

        private volatile Instant now = Instant.EPOCH;

        void set(String instant) {
            this.now = Instant.parse(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClock {

        @Bean
        @Primary
        SettableClock settableClock() {
            return new SettableClock();
        }
    }

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AppointmentDeadlines deadlines;

    @Autowired
    private SettableClock clock;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void anOffering() {
        TestBookings.insertOneHourClairvoyanceOffering(jdbc);
    }

    @Test
    void theClientAndThePractitionerAreToldAtEachStep() throws Exception {
        clock.set("2026-10-18T17:30:00Z"); // Sunday 18 October, 19:30 in Paris
        var id = book("anna@example.com", "2026-10-19T14:00:00+02:00");

        assertThat(mvc.post().uri("/api/admin/appointments/{id}/confirm", id).with(practitioner()).with(csrf()))
            .hasStatusOk();
        assertThat(awaitMail("anna@example.com", "Rendez-vous confirmé").getText())
            .contains("Lundi 19 octobre à 14h00")
            .contains("sur le compte « Alice Martin »");

        assertThat(deadlines.sendReminders()).isEqualTo(1);
        assertThat(awaitMail("anna@example.com", "Rappel").getSubject())
            .isEqualTo("Rappel : votre séance demain à 14h00");

        assertThat(deadlines.publishEveningProgram()).isTrue();
        assertThat(awaitMail("so@example.com", "Votre programme").getText()).contains("- 14h00 : Alice Martin");
    }

    @Test
    void anUnansweredRequestExpiresTheEveningBefore() throws Exception {
        clock.set("2026-10-20T17:30:00Z"); // Tuesday 20 October, 19:30 in Paris
        book("bruno@example.com", "2026-10-21T14:00:00+02:00");
        assertThat(deadlines.expireUnansweredRequests()).isZero();

        clock.set("2026-10-20T19:05:00Z"); // 21:05
        assertThat(deadlines.expireUnansweredRequests()).isEqualTo(1);

        assertThat(awaitMail("bruno@example.com", "Votre demande").getText())
            .contains("n'a pas pu être confirmée à temps");
        assertThat(mvc.get().uri("/api/offerings/consultation-1-h/slots?from=2026-10-21&to=2026-10-21"))
            .bodyJson()
            .extractingPath("$.length()").isEqualTo(3);
    }

    @Test
    void aWithdrawnRequestIsConfirmedToTheClientAndToldToThePractitioner() throws Exception {
        clock.set("2026-10-18T08:00:00Z"); // Sunday 18 October, 10:00 in Paris
        var id = book("chloe@example.com", "2026-10-22T14:00:00+02:00");

        assertThat(mvc.post().uri("/api/bookings/{id}/cancel", id).with(user("chloe@example.com")).with(csrf()))
            .hasStatusOk();

        assertThat(awaitMail("chloe@example.com", "Annulation enregistrée").getText())
            .contains("est bien annulée");
        assertThat(awaitMail("so@example.com", "Demande retirée").getText())
            .contains("Alice Martin a retiré sa demande");
    }

    /** Books, then waits for the practitioner's email about this request, with the link to answer it. */
    private long book(String email, String start) throws Exception {
        var result = mvc.perform(TestBookings.bookingRequest(start, true).with(user(email)).with(csrf()));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        awaitMail("so@example.com", "Nouvelle demande", "/admin/agenda/" + id);
        return id.longValue();
    }

    private SimpleMailMessage awaitMail(String to, String subjectStart) throws InterruptedException {
        return awaitMail(to, subjectStart, "");
    }

    /**
     * Emails leave after the commit, in another thread: waits for the expected one, four seconds at most.
     * Mockito records every call to the mocked sender, whatever the thread.
     */
    private SimpleMailMessage awaitMail(String to, String subjectStart, String textPart)
        throws InterruptedException {
        for (var attempt = 0; attempt < 40; attempt++) {
            Optional<SimpleMailMessage> found = mockingDetails(mailSender).getInvocations().stream()
                .flatMap(invocation -> Arrays.stream(invocation.getArguments()))
                .filter(SimpleMailMessage.class::isInstance)
                .map(SimpleMailMessage.class::cast)
                .filter(mail -> mail.getTo() != null && Arrays.asList(mail.getTo()).contains(to))
                .filter(mail -> mail.getSubject() != null && mail.getSubject().startsWith(subjectStart))
                .filter(mail -> mail.getText() != null && mail.getText().contains(textPart))
                .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No email \"" + subjectStart + "…\" to " + to);
    }
}
