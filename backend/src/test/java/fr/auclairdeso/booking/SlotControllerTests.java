package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import fr.auclairdeso.WebSecurityTestConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SlotController.class)
@Import(WebSecurityTestConfiguration.class)
class SlotControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private Availability availability;

    @Test
    void listsTheSlotsOfAnOfferingToAnyVisitor() {
        var monday = LocalDate.of(2026, 10, 12);
        given(availability.forOffering("guidance-1-h", monday, monday)).willReturn(Optional.of(List.of(
            new TimeSlot(OffsetDateTime.parse("2026-10-12T14:00:00+02:00"),
                OffsetDateTime.parse("2026-10-12T15:00:00+02:00")))));

        assertThat(mvc.get().uri("/api/offerings/guidance-1-h/slots?from=2026-10-12&to=2026-10-12"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$[0].start").asString()
            .satisfies(start -> assertThat(OffsetDateTime.parse(start).toInstant())
                .isEqualTo(Instant.parse("2026-10-12T12:00:00Z")));
    }

    @Test
    void rejectsAPeriodGoingBackwards() {
        assertThat(mvc.get().uri("/api/offerings/guidance-1-h/slots?from=2026-10-13&to=2026-10-12"))
            .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
