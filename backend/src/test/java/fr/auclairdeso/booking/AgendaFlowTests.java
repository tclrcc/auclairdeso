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

    private long book(String email, String start) throws Exception {
        var result = mvc.perform(TestBookings.bookingRequest(start, true).with(user(email)).with(csrf()));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
