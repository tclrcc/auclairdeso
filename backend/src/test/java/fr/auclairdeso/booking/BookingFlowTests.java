package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import fr.auclairdeso.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class BookingFlowTests {

    /** Today is Sunday 11 October 2026, 10:00 in Paris: Monday 12 to Thursday 15 are bookable. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FrozenClock {

        @Bean
        @Primary
        Clock frozenClock() {
            return Clock.fixed(Instant.parse("2026-10-11T08:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void anOfferingOfOneHourWithAQuarterHourBreak() {
        jdbc.update("""
                INSERT INTO offering (slug, name, description, category, duration_minutes, buffer_minutes,
                                      price_cents, payment_policy, display_order, active)
                VALUES ('consultation-1-h', 'Consultation approfondie', 'Une heure.', 'CLAIRVOYANCE', 60, 15,
                        8000, 'ON_SITE', 0, TRUE)
                ON CONFLICT (slug) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO offering_mode (offering_id, mode)
                SELECT id, 'VIDEO' FROM offering WHERE slug = 'consultation-1-h'
                ON CONFLICT DO NOTHING
                """);
    }

    @Test
    void aRequestHoldsItsSlot() {
        assertThat(book("alice@example.com", "2026-10-12T14:00:00+02:00", true)).hasStatus(HttpStatus.CREATED);

        // 14:00 is taken: the chain resumes at 15:15, then 16:30.
        assertThat(mvc.get().uri("/api/offerings/consultation-1-h/slots?from=2026-10-12&to=2026-10-12"))
            .bodyJson()
            .extractingPath("$.length()").isEqualTo(2);
    }

    @Test
    void twoClientsCannotTakeTheSameSlot() {
        assertThat(book("bob@example.com", "2026-10-13T14:00:00+02:00", true)).hasStatus(HttpStatus.CREATED);
        assertThat(book("carol@example.com", "2026-10-13T14:00:00+02:00", true)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void aClientHasOnlyOneUpcomingBooking() {
        assertThat(book("dave@example.com", "2026-10-14T14:00:00+02:00", true)).hasStatus(HttpStatus.CREATED);
        assertThat(book("dave@example.com", "2026-10-14T16:30:00+02:00", true)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void clairvoyanceNeedsAPhoto() {
        assertThat(book("eve@example.com", "2026-10-14T15:15:00+02:00", false)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theDatabaseItselfRefusesOverlappingSessions() {
        var clientId = jdbc.queryForObject("""
                INSERT INTO client (email, first_name, last_name, birth_date, phone)
                VALUES ('direct@example.com', 'Dora', 'Direct', '1980-01-01', '0600000000')
                RETURNING id
                """, Long.class);
        var insert = """
                INSERT INTO appointment (client_id, offering_slug, offering_name, price_cents, mode, starts_at,
                                         ends_at, occupied_until, status, reason, messenger_name, consented_at)
                VALUES (?, 'consultation-1-h', 'Consultation', 8000, 'VIDEO', ?::timestamptz, ?::timestamptz,
                        ?::timestamptz, 'REQUESTED', 'Test', 'Dora', now())
                """;
        jdbc.update(insert, clientId, "2026-10-15 14:00+02", "2026-10-15 15:00+02", "2026-10-15 15:15+02");

        // Starts during the break of the first session: refused, whatever the application checked.
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
            jdbc.update(insert, clientId, "2026-10-15 15:00+02", "2026-10-15 16:00+02", "2026-10-15 16:15+02"));
    }

    private MvcTestResult book(String email, String start, boolean withPhoto) {
        var json = """
                {"offeringSlug": "consultation-1-h", "mode": "VIDEO", "start": "%s",
                 "client": {"firstName": "Alice", "lastName": "Martin", "birthDate": "1990-05-04",
                            "phone": "06 12 34 56 78", "city": null},
                 "reason": "Une question sur mon avenir professionnel.", "address": null,
                 "messengerName": "Alice Martin", "acceptedTerms": true, "consentsToDataProcessing": true}
                """.formatted(start);
        var request = multipart("/api/bookings").file(new MockMultipartFile("request", "",
            MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8)));
        if (withPhoto) {
            request.file(new MockMultipartFile("photo", "portrait.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG));
        }
        return mvc.perform(request.with(user(email)).with(csrf()));
    }
}
