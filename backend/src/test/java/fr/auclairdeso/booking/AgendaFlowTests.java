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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AgendaFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class AgendaFlowTests {

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

    @BeforeEach
    void anOffering() {
        TestBookings.insertOneHourClairvoyanceOffering(jdbc);
    }

    @Test
    void thePractitionerConfirmsARequestAndTheClientSeesIt() throws Exception {
        var id = book("anna@example.com", "2026-10-19T14:00:00+02:00");

        assertThat(mvc.get().uri("/api/admin/appointments/pending").with(practitioner()))
            .bodyJson()
            .extractingPath("$[*].client.email").asArray().contains("anna@example.com");

        assertThat(mvc.post().uri("/api/admin/appointments/{id}/confirm", id).with(practitioner()).with(csrf()))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.status").isEqualTo("CONFIRMED");
        assertThat(mvc.get().uri("/api/bookings/mine").with(user("anna@example.com")))
            .bodyJson()
            .extractingPath("$[0].status").isEqualTo("CONFIRMED");
    }

    @Test
    void aDeclinedRequestFreesItsSlotAtOnce() throws Exception {
        var id = book("bruno@example.com", "2026-10-20T14:00:00+02:00");
        assertThat(mvc.get().uri("/api/offerings/consultation-1-h/slots?from=2026-10-20&to=2026-10-20"))
            .bodyJson()
            .extractingPath("$.length()").isEqualTo(2);

        assertThat(mvc.post().uri("/api/admin/appointments/{id}/decline", id).with(practitioner()).with(csrf()))
            .hasStatusOk();

        assertThat(mvc.get().uri("/api/offerings/consultation-1-h/slots?from=2026-10-20&to=2026-10-20"))
            .bodyJson()
            .extractingPath("$.length()").isEqualTo(3);
    }

    @Test
    void onlyStaffCanSeeThePhoto() throws Exception {
        var id = book("chloe@example.com", "2026-10-21T14:00:00+02:00");

        assertThat(mvc.get().uri("/api/admin/appointments/{id}/photo", id).with(user("chloe@example.com")))
            .hasStatus(HttpStatus.FORBIDDEN);

        var photo = mvc.get().uri("/api/admin/appointments/{id}/photo", id).with(practitioner()).exchange();
        assertThat(photo).hasStatusOk().hasContentType(MediaType.IMAGE_JPEG);
        assertThat(photo.getResponse().getContentAsByteArray()).isEqualTo(TestBookings.JPEG);
    }

    @Test
    void twoLateCancellationsBlockTheClient() throws Exception {
        for (var round = 1; round <= 2; round++) {
            var id = book("dora@example.com", "2026-10-19T15:15:00+02:00");
            assertThat(mvc.post().uri("/api/admin/appointments/{id}/confirm", id).with(practitioner()).with(csrf()))
                .hasStatusOk();
            assertThat(mvc.post().uri("/api/bookings/{id}/cancel", id).with(user("dora@example.com")).with(csrf()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.lateCancellation").isEqualTo(true);
        }

        assertThat(mvc.perform(TestBookings.bookingRequest("2026-10-19T15:15:00+02:00", true)
            .with(user("dora@example.com")).with(csrf())))
            .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void aNoShowBlocksTheClient() throws Exception {
        var id = pastConfirmedSession("eric@example.com");
        assertThat(mvc.get().uri("/api/admin/appointments/to-close").with(practitioner()))
            .bodyJson()
            .extractingPath("$[*].client.email").asArray().contains("eric@example.com");

        assertThat(mvc.post().uri("/api/admin/appointments/{id}/no-show", id).with(practitioner()).with(csrf()))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.status").isEqualTo("NO_SHOW");

        assertThat(mvc.perform(TestBookings.bookingRequest("2026-10-21T15:15:00+02:00", true)
            .with(user("eric@example.com")).with(csrf())))
            .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void thePractitionerCanBookOutsideOpeningHoursButNeverOverAnotherSession() {
        var urgent = """
                {"offeringSlug": "consultation-1-h", "mode": "VIDEO", "start": "2026-10-23T19:00",
                 "newClient": {"firstName": "Louise", "lastName": "Urgent", "phone": "06 11 22 33 44"},
                 "messengerName": "Louise U", "note": "Appel en urgence"}
                """;

        assertThat(mvc.post().uri("/api/admin/appointments").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(urgent))
            .hasStatus(HttpStatus.CREATED)
            .bodyJson()
            .extractingPath("$.status").isEqualTo("CONFIRMED");

        assertThat(mvc.post().uri("/api/admin/appointments").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(urgent))
            .hasStatus(HttpStatus.CONFLICT);

        assertThat(mvc.get().uri("/api/admin/clients?query=urgent").with(practitioner()))
            .bodyJson()
            .extractingPath("$[*].lastName").asArray().contains("Urgent");
    }

    /** A confirmed session on Friday 16 October, before the frozen "today". */
    private long pastConfirmedSession(String email) {
        var clientId = jdbc.queryForObject("""
                INSERT INTO client (email, first_name, last_name, birth_date, phone)
                VALUES (?, 'Eric', 'Absent', '1985-03-02', '0600000000')
                RETURNING id
                """, Long.class, email);
        return jdbc.queryForObject("""
                INSERT INTO appointment (client_id, offering_slug, offering_name, price_cents, mode, starts_at,
                                         ends_at, occupied_until, status, reason, messenger_name, consented_at)
                VALUES (?, 'consultation-1-h', 'Consultation', 8000, 'VIDEO', '2026-10-16 14:00+02',
                        '2026-10-16 15:00+02', '2026-10-16 15:15+02', 'CONFIRMED', 'Test', 'Eric', now())
                RETURNING id
                """, Long.class, clientId);
    }

    private long book(String email, String start) throws Exception {
        var result = mvc.perform(TestBookings.bookingRequest(start, true).with(user(email)).with(csrf()));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
