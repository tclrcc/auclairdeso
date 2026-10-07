package fr.auclairdeso.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

final class TestBookings {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private TestBookings() {
    }

    /** "consultation-1-h": clairvoyance, one hour, a quarter of an hour of break, by video. */
    static void insertOneHourClairvoyanceOffering(JdbcTemplate jdbc) {
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

    static MockMultipartHttpServletRequestBuilder bookingRequest(String start, boolean withPhoto) {
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
        return request;
    }
}
