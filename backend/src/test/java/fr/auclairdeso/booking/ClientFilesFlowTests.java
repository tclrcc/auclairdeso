package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static fr.auclairdeso.TestcontainersConfiguration.NO_MAIL_HEALTH_CHECK;

import com.jayway.jsonpath.JsonPath;
import fr.auclairdeso.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = NO_MAIL_HEALTH_CHECK)
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ClientFilesFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class ClientFilesFlowTests {

    /** Today is Sunday 18 October 2026, 10:00 in Paris. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FrozenClock {

        @Bean
        @Primary
        Clock frozenClock() {
            return Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void anOffering() {
        TestBookings.insertOneHourClairvoyanceOffering(jdbc);
    }

    @Test
    void theFileShowsTheHistoryAndThePrivateNotes() throws Exception {
        var clientId = clientOf(book("fanny@example.com", "2026-10-19T14:00:00+02:00", "06 55 44 33 22"));

        var file = mvc.get().uri("/api/admin/clients/{id}", clientId).with(practitioner()).exchange();
        assertThat(file).bodyJson().extractingPath("$.appointments[0].status").isEqualTo("REQUESTED");
        assertThat(file).bodyJson().extractingPath("$.bookedOnline").isEqualTo(true);

        var note = mvc.post().uri("/api/admin/clients/{id}/notes", clientId).with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"text": "Préfère être appelée le matin."}
                """)
            .exchange();
        assertThat(note).hasStatus(HttpStatus.CREATED);
        Number noteId = JsonPath.read(note.getResponse().getContentAsString(), "$.id");

        assertThat(mvc.get().uri("/api/admin/clients/{id}", clientId).with(practitioner()))
            .bodyJson()
            .extractingPath("$.notes[0].text").isEqualTo("Préfère être appelée le matin.");

        // The note of a file cannot be deleted through another file.
        assertThat(mvc.delete().uri("/api/admin/clients/{id}/notes/{noteId}", clientId + 1000, noteId)
            .with(practitioner()).with(csrf()))
            .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/admin/clients/{id}/notes/{noteId}", clientId, noteId)
            .with(practitioner()).with(csrf()))
            .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/admin/clients/{id}", clientId).with(practitioner()))
            .bodyJson()
            .extractingPath("$.notes").asArray().isEmpty();
    }

    @Test
    void theBlockFollowsThePhoneUntilThePractitionerLiftsIt() throws Exception {
        var appointmentId = book("gina@example.com", "2026-10-20T14:00:00+02:00", "06 98 76 54 32");
        var clientId = clientOf(appointmentId);
        // Her sister gave the same number, written another way.
        var sisterId = clientOf(book("ines@example.com", "2026-10-21T15:15:00+02:00", "06.98.76.54.32"));
        assertThat(mvc.post().uri("/api/admin/appointments/{id}/decline", appointmentId)
            .with(practitioner()).with(csrf()))
            .hasStatusOk();

        assertThat(mvc.post().uri("/api/admin/clients/{id}/block", clientId).with(practitioner()).with(csrf()))
            .bodyJson()
            .extractingPath("$.client.blocked").isEqualTo(true);

        assertThat(request("gina@example.com", "2026-10-20T14:00:00+02:00", "06 98 76 54 32"))
            .hasStatus(HttpStatus.FORBIDDEN);
        // Another email, the same number written another way: still refused.
        assertThat(request("gina.bis@example.com", "2026-10-20T14:00:00+02:00", "+33 6 98 76 54 32"))
            .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/clients?filter=BLOCKED").with(practitioner()))
            .bodyJson()
            .extractingPath("$[*].email").asArray().containsExactly("gina@example.com");
        // The sister is not blocked herself, but her file says why she cannot book online any more.
        var sister = mvc.get().uri("/api/admin/clients/{id}", sisterId).with(practitioner()).exchange();
        assertThat(sister).bodyJson().extractingPath("$.client.blocked").isEqualTo(false);
        assertThat(sister).bodyJson().extractingPath("$.phoneBlocked").isEqualTo(true);

        var unblocked = mvc.post().uri("/api/admin/clients/{id}/unblock", clientId).with(practitioner()).with(csrf())
            .exchange();
        assertThat(unblocked).bodyJson().extractingPath("$.client.blocked").isEqualTo(false);
        assertThat(unblocked).bodyJson().extractingPath("$.lateCancellations").isEqualTo(0);
        assertThat(request("gina@example.com", "2026-10-20T14:00:00+02:00", "06 98 76 54 32"))
            .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void aClientEnteredByPhoneMayReceiveAnEmailButAnOnlineClientKeepsHers() throws Exception {
        var manual = mvc.post().uri("/api/admin/appointments").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"offeringSlug": "consultation-1-h", "mode": "VIDEO", "start": "2026-10-23T19:00",
                 "newClient": {"firstName": "Louise", "lastName": "Durand", "phone": "06 11 22 33 44"},
                 "messengerName": "Louise D", "note": "Appel en urgence"}
                """)
            .exchange();
        assertThat(manual).hasStatus(HttpStatus.CREATED);
        Number louise = JsonPath.read(manual.getResponse().getContentAsString(), "$.client.id");

        assertThat(correct(louise.longValue(), "Louise", "Durand", "louise@example.com"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.client.email").isEqualTo("louise@example.com");
        assertThat(mvc.post().uri("/api/admin/clients/{id}/trust", louise).with(practitioner()).with(csrf()))
            .bodyJson()
            .extractingPath("$.client.trusted").isEqualTo(true);

        var hannah = clientOf(book("hannah@example.com", "2026-10-21T14:00:00+02:00", "06 22 33 44 55"));
        assertThat(correct(hannah, "Hannah", "Martin", "autre@example.com")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(correct(hannah, "Hanna", "Martin", "hannah@example.com"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.client.firstName").isEqualTo("Hanna");
    }

    private MvcTestResult correct(long clientId, String firstName, String lastName, String email) {
        return mvc.put().uri("/api/admin/clients/{id}", clientId).with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"firstName": "%s", "lastName": "%s", "birthDate": null, "phone": "06 11 22 33 44",
                 "city": "Lyon", "email": "%s"}
                """.formatted(firstName, lastName, email))
            .exchange();
    }

    private MvcTestResult request(String email, String start, String phone) {
        return mvc.perform(TestBookings.bookingRequest(start, true, phone).with(user(email)).with(csrf()));
    }

    private long book(String email, String start, String phone) throws Exception {
        var result = request(email, start, phone);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long clientOf(long appointmentId) throws Exception {
        var appointment = mvc.get().uri("/api/admin/appointments/{id}", appointmentId).with(practitioner()).exchange();
        Number id = JsonPath.read(appointment.getResponse().getContentAsString(), "$.client.id");
        return id.longValue();
    }
}
