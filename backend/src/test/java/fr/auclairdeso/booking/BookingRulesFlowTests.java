package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static fr.auclairdeso.TestcontainersConfiguration.NO_MAIL_HEALTH_CHECK;
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
 * The refusals of a booking request that depend on who asks and on the day (docs/specification.md, 3.4, 4.3
 * and 5.3). Each test books on its own day, so that none of them changes the slots of another.
 */
@SpringBootTest(properties = NO_MAIL_HEALTH_CHECK)
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingRulesFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class BookingRulesFlowTests {

    /** Today is Sunday 11 October 2026, 10:00 in Paris: Monday 12 to Thursday 15 are bookable. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FrozenClock {

        @Bean
        @Primary
        Clock frozenClock() {
            return Clock.fixed(Instant.parse("2026-10-11T08:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final String ADULT = "1990-05-04";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender mailSender;

    /** Magnetism needs no photo; the half-hour offering has no break, so a day holds eight of its slots. */
    @BeforeEach
    void offerings() {
        offering("seance-courte", 30, 0, "VIDEO");
        offering("seance-cabinet", 60, 15, "IN_PERSON");
        offering("seance-domicile", 60, 15, "CLIENT_HOME");
    }

    @Test
    void aDayHoldsThreeSessionsAtMostAndACancellationFreesAPlace() throws Exception {
        var first = book("camille@example.com", "2026-10-13T14:00:00+02:00");
        book("clara@example.com", "2026-10-13T14:30:00+02:00");
        book("cleo@example.com", "2026-10-13T15:00:00+02:00");

        // 15:30 is free in the chain, but the day is full.
        assertThat(request("coralie@example.com", "2026-10-13T15:30:00+02:00")).hasStatus(HttpStatus.CONFLICT);

        assertThat(mvc.post().uri("/api/bookings/{id}/cancel", first).with(user("camille@example.com")).with(csrf()))
            .hasStatusOk();

        assertThat(request("coralie@example.com", "2026-10-13T15:30:00+02:00")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void aMinorCannotBookButSomeoneTurningEighteenTodayCan() {
        // Born on 12 October 2008: she turns 18 tomorrow.
        assertThat(mvc.perform(TestBookings.bookingRequest("seance-courte", "VIDEO", "2026-10-12T14:00:00+02:00",
            "2008-10-12", null, false, "0700000101").with(user("manon@example.com")).with(csrf())))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.detail").isEqualTo("Les séances sont réservées aux personnes majeures.");

        assertThat(mvc.perform(TestBookings.bookingRequest("seance-courte", "VIDEO", "2026-10-12T14:00:00+02:00",
            "2008-10-11", null, false, "0700000102").with(user("maelle@example.com")).with(csrf())))
            .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void inPersonIsForTrustedClientsOnly() {
        var clientId = jdbc.queryForObject("""
                INSERT INTO client (email, first_name, last_name, birth_date, phone)
                VALUES ('prisca@example.com', 'Prisca', 'Martin', '1985-03-02', '0700000103')
                RETURNING id
                """, Long.class);

        assertThat(inPerson("prisca@example.com", "2026-10-14T14:00:00+02:00"))
            .hasStatus(HttpStatus.FORBIDDEN)
            .bodyJson()
            .extractingPath("$.detail").isEqualTo("Les séances en présentiel sont réservées aux personnes déjà suivies.");

        assertThat(mvc.post().uri("/api/admin/clients/{id}/trust", clientId).with(practitioner()).with(csrf()))
            .hasStatusOk();

        assertThat(inPerson("prisca@example.com", "2026-10-14T14:00:00+02:00")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void aSessionAtHomeNeedsTheAddress() {
        assertThat(atHome("helene@example.com", null))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.detail").isEqualTo("Indiquez l'adresse de la séance à domicile.");

        assertThat(atHome("helene@example.com", "3 rue des Lilas, Lagnieu")).hasStatus(HttpStatus.CREATED);
    }

    private void offering(String slug, int durationMinutes, int bufferMinutes, String mode) {
        jdbc.update("""
                INSERT INTO offering (slug, name, description, category, duration_minutes, buffer_minutes,
                                      price_cents, payment_policy, display_order, active)
                VALUES (?, ?, 'Test.', 'MAGNETISM', ?, ?, 6000, 'ON_SITE', 0, TRUE)
                ON CONFLICT (slug) DO NOTHING
                """, slug, slug, durationMinutes, bufferMinutes);
        jdbc.update("""
                INSERT INTO offering_mode (offering_id, mode)
                SELECT id, ? FROM offering WHERE slug = ?
                ON CONFLICT DO NOTHING
                """, mode, slug);
    }

    private long book(String email, String start) throws Exception {
        var result = request(email, start);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private MvcTestResult request(String email, String start) {
        return mvc.perform(TestBookings.bookingRequest("seance-courte", "VIDEO", start, ADULT, null, false,
            phoneOf(email)).with(user(email)).with(csrf()));
    }

    private MvcTestResult inPerson(String email, String start) {
        return mvc.perform(TestBookings.bookingRequest("seance-cabinet", "IN_PERSON", start, ADULT, null, false,
            phoneOf(email)).with(user(email)).with(csrf()));
    }

    private MvcTestResult atHome(String email, String address) {
        return mvc.perform(TestBookings.bookingRequest("seance-domicile", "CLIENT_HOME", "2026-10-15T14:00:00+02:00",
            ADULT, address, false, phoneOf(email)).with(user(email)).with(csrf()));
    }

    /** A phone number made from the email, so that two clients never share one and a client keeps hers. */
    private static String phoneOf(String email) {
        return "07%08d".formatted(Math.abs(email.hashCode()) % 100_000_000);
    }
}
