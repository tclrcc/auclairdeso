package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The right to erasure (docs/specification.md, 9), and the nightly deletion of the portraits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AccountErasureFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class AccountErasureFlowTests {

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

    @Autowired
    private AppointmentDeadlines deadlines;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void anOffering() {
        TestBookings.insertOneHourClairvoyanceOffering(jdbc);
    }

    @Test
    void aClientWithASessionToComeCancelsItBeforeSheCanErase() throws Exception {
        var id = book("iris@example.com", "2026-10-19T14:00:00+02:00");

        assertThat(erase("iris@example.com"))
            .hasStatus(HttpStatus.CONFLICT)
            .bodyJson()
            .extractingPath("$.detail").asString().contains("Annulez d'abord");

        assertThat(mvc.post().uri("/api/bookings/{id}/cancel", id).with(user("iris@example.com")).with(csrf()))
            .hasStatusOk();
        assertThat(erase("iris@example.com")).hasStatusOk();
    }

    @Test
    void erasingKeepsTheSessionButNothingThatNamesTheClient() throws Exception {
        var id = book("jana@example.com", "2026-10-20T14:00:00+02:00");
        assertThat(mvc.post().uri("/api/admin/appointments/{id}/decline", id).with(practitioner()).with(csrf()))
            .hasStatusOk();
        var clientId = clientOf(id);
        jdbc.update("INSERT INTO user_account (email, role) VALUES (?, 'CLIENT')", "jana@example.com");
        assertThat(mvc.post().uri("/api/admin/clients/{id}/notes", clientId).with(practitioner()).with(csrf())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"text": "Une note privée."}
                """))
            .hasStatus(HttpStatus.CREATED);

        assertThat(erase("jana@example.com"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.appointments").isEqualTo(1);

        // The session is still there for the practitioner, emptied of the client.
        var file = mvc.get().uri("/api/admin/clients/{id}", clientId).with(practitioner()).exchange();
        assertThat(file).bodyJson().extractingPath("$.client.email").isNull();
        assertThat(file).bodyJson().extractingPath("$.client.erased").isEqualTo(true);
        assertThat(file).bodyJson().extractingPath("$.notes").asArray().isEmpty();
        assertThat(file).bodyJson().extractingPath("$.appointments.length()").isEqualTo(1);
        assertThat(mvc.get().uri("/api/admin/appointments/{id}", id).with(practitioner()))
            .bodyJson()
            .extractingPath("$.reason").isEqualTo("Effacé à la demande de la cliente.");
        // Her portrait is gone at once, and her login follows the commit of the erasure.
        assertThat(mvc.get().uri("/api/admin/appointments/{id}/photo", id).with(practitioner()))
            .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(awaitLoginDeleted("jana@example.com")).isTrue();

        // What is left of her file cannot be written back into existence.
        assertThat(mvc.post().uri("/api/admin/clients/{id}/trust", clientId).with(practitioner()).with(csrf()))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void aPortraitIsDeletedOnceItsSessionIsOldEnough() {
        var recent = pastSession("2026-10-16 14:00+02");
        var old = pastSession("2026-10-01 14:00+02");

        assertThat(deadlines.deleteExpiredPhotos()).isEqualTo(1);

        assertThat(hasPhoto(old)).isFalse();
        assertThat(hasPhoto(recent)).isTrue();
    }

    /** The login is deleted by a listener that runs once the erasure is committed: four seconds at most. */
    private boolean awaitLoginDeleted(String email) throws InterruptedException {
        for (var attempt = 0; attempt < 40; attempt++) {
            if (jdbc.queryForObject("SELECT count(*) FROM user_account WHERE email = ?", Integer.class, email) == 0) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private MvcTestResult erase(String email) {
        return mvc.delete().uri("/api/account").with(user(email).roles("CLIENT")).with(csrf()).exchange();
    }

    private long book(String email, String start) throws Exception {
        var result = mvc.perform(TestBookings.bookingRequest(start, true).with(user(email)).with(csrf()));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long clientOf(long appointmentId) throws Exception {
        var appointment = mvc.get().uri("/api/admin/appointments/{id}", appointmentId).with(practitioner()).exchange();
        Number id = JsonPath.read(appointment.getResponse().getContentAsString(), "$.client.id");
        return id.longValue();
    }

    /** A session that already took place, with its portrait, entered straight into the database. */
    private long pastSession(String start) {
        var clientId = jdbc.queryForObject("""
                INSERT INTO client (email, first_name, last_name, birth_date, phone)
                VALUES (?, 'Test', 'Client', '1985-03-02', '0600000000')
                RETURNING id
                """, Long.class, start + "@example.com");
        var appointmentId = jdbc.queryForObject("""
                INSERT INTO appointment (client_id, offering_slug, offering_name, price_cents, mode, starts_at,
                                         ends_at, occupied_until, status, source, reason, messenger_name,
                                         consented_at)
                VALUES (?, 'consultation-1-h', 'Consultation approfondie', 8000, 'VIDEO', ?::timestamptz,
                        ?::timestamptz + interval '1 hour', ?::timestamptz + interval '75 minutes',
                        'COMPLETED', 'ONLINE', 'Test', 'Client', now())
                RETURNING id
                """, Long.class, clientId, start, start, start);
        jdbc.update("""
                INSERT INTO appointment_photo (appointment_id, content_type, data)
                VALUES (?, 'image/jpeg', ?)
                """, appointmentId, TestBookings.JPEG);
        return appointmentId;
    }

    private boolean hasPhoto(long appointmentId) {
        return jdbc.queryForObject("SELECT count(*) FROM appointment_photo WHERE appointment_id = ?", Integer.class,
            appointmentId) == 1;
    }
}
