package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import fr.auclairdeso.WebSecurityTestConfiguration;
import fr.auclairdeso.catalog.ConsultationMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BookingController.class)
@Import(WebSecurityTestConfiguration.class)
class BookingControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BookingRequests bookings;

    @Test
    void anonymousVisitorsMustLogInFirst() {
        assertThat(mvc.perform(multipart("/api/bookings").file(requestPart(true)).with(csrf())))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void consentsAreRequired() {
        assertThat(mvc.perform(multipart("/api/bookings").file(requestPart(false))
            .with(user("alice@example.com")).with(csrf())))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.errors[*].field")
            .asArray()
            .contains("acceptedTerms", "consentsToDataProcessing");
    }

    @Test
    void aValidRequestIsCreatedForTheLoggedInClient() {
        given(bookings.request(eq("alice@example.com"), any(), any())).willReturn(new AppointmentView(1L,
            "consultation-1-h", "Consultation approfondie", ConsultationMode.VIDEO,
            OffsetDateTime.parse("2026-10-12T14:00:00+02:00"), OffsetDateTime.parse("2026-10-12T15:00:00+02:00"),
            AppointmentStatus.REQUESTED, 8_000, OffsetDateTime.parse("2026-10-10T14:00:00+02:00"), false));

        assertThat(mvc.perform(multipart("/api/bookings").file(requestPart(true))
            .with(user("alice@example.com")).with(csrf())))
            .hasStatus(HttpStatus.CREATED)
            .bodyJson()
            .extractingPath("$.status").isEqualTo("REQUESTED");
    }

    private static MockMultipartFile requestPart(boolean consents) {
        var json = """
                {"offeringSlug": "consultation-1-h", "mode": "VIDEO", "start": "2026-10-12T14:00:00+02:00",
                 "client": {"firstName": "Alice", "lastName": "Martin", "birthDate": "1990-05-04",
                            "phone": "06 12 34 56 78", "city": null},
                 "reason": "Une question sur mon avenir professionnel.", "address": null,
                 "messengerName": "Alice Martin", "acceptedTerms": %s, "consentsToDataProcessing": %s}
                """.formatted(consents, consents);
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
            json.getBytes(StandardCharsets.UTF_8));
    }
}
