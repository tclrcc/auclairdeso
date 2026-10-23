package fr.auclairdeso.billing;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static fr.auclairdeso.TestcontainersConfiguration.NO_MAIL_HEALTH_CHECK;

import com.jayway.jsonpath.JsonPath;
import fr.auclairdeso.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * From a completed session to the receipt book: the bill is issued, numbered, emailed, then paid.
 */
@SpringBootTest(properties = {
    NO_MAIL_HEALTH_CHECK,
    "auclairdeso.billing.practitioner-name=Sophie Exemple EI",
    "auclairdeso.billing.address=1 place de la Mairie, 01150 Chazey-sur-Ain",
    "auclairdeso.billing.siret=123 456 789 00012"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BillingFlowTests.FrozenClock.class})
@Testcontainers(disabledWithoutDocker = true)
class BillingFlowTests {

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
        jdbc.update("""
                INSERT INTO offering (slug, name, description, category, duration_minutes, buffer_minutes,
                                      price_cents, payment_policy, display_order, active)
                VALUES ('consultation-1-h', 'Consultation approfondie', 'Une heure.', 'CLAIRVOYANCE', 60, 15,
                        8000, 'ON_SITE', 0, TRUE)
                ON CONFLICT (slug) DO NOTHING
                """);
    }

    @Test
    void aCompletedSessionGetsANumberedBillThatTheClientReceives() throws Exception {
        var first = pastConfirmedSession("ana@example.com", "Ana", "2026-10-16 10:00+02");
        var second = pastConfirmedSession("bea@example.com", "Béa", "2026-10-16 14:00+02");

        assertThat(complete(first)).hasStatusOk();
        assertThat(complete(second)).hasStatusOk();

        // Whatever the order of the tests: numbers of this year, each one following the previous.
        var bill = billOf(first);
        String number = JsonPath.read(bill.getResponse().getContentAsString(), "$[0].number");
        assertThat(number).matches("2026-\\d{4}");
        assertThat(bill).bodyJson().extractingPath("$[0].amountCents").isEqualTo(8000);
        var next = "2026-%04d".formatted(Integer.parseInt(number.substring(5)) + 1);
        assertThat(billOf(second)).bodyJson().extractingPath("$[0].number").isEqualTo(next);

        assertThat(awaitMail("ana@example.com", "Votre note n° " + number).getText())
            .contains("Sophie Exemple EI")
            .contains("1 place de la Mairie, 01150 Chazey-sur-Ain")
            .contains("Séance du vendredi 16 octobre 2026 à 10h00")
            .contains("Total à payer : 80,00 €")
            .contains("TVA non applicable, article 293 B du CGI");
    }

    @Test
    void aPaymentGoesIntoTheReceiptBookUntilItIsCancelled() throws Exception {
        var session = pastConfirmedSession("chloe@example.com", "Chloé", "2026-10-17 10:00+02");
        assertThat(complete(session)).hasStatusOk();
        Number billId = JsonPath.read(billOf(session).getResponse().getContentAsString(), "$[0].id");

        assertThat(mvc.get().uri("/api/admin/bills/unpaid").with(practitioner()))
            .bodyJson()
            .extractingPath("$[*].id").asArray().contains(billId.intValue());
        assertThat(pay(billId, "2026-10-19")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(pay(billId, "2026-10-18")).hasStatusOk();
        assertThat(pay(billId, "2026-10-18")).hasStatus(HttpStatus.CONFLICT);

        var book = mvc.get().uri("/api/admin/receipts?year=2026").with(practitioner()).exchange();
        assertThat(book).bodyJson().extractingPath("$.entries[*].clientName").asArray().contains("Chloé Client");
        assertThat(book).bodyJson().extractingPath("$.totalsByMethod.TRANSFER").isEqualTo(8000);
        assertThat(mvc.get().uri("/api/admin/receipts/export?year=2026").with(practitioner()))
            .bodyText()
            .contains("18/10/2026;");

        assertThat(mvc.delete().uri("/api/admin/bills/{id}/payment", billId).with(practitioner()).with(csrf()))
            .hasStatusOk();
        assertThat(mvc.get().uri("/api/admin/receipts?year=2026").with(practitioner()))
            .bodyJson()
            .extractingPath("$.entries[*].clientName").asArray().doesNotContain("Chloé Client");
    }

    /** A confirmed session that has already taken place, entered straight into the database. */
    private long pastConfirmedSession(String email, String firstName, String start) {
        var clientId = jdbc.queryForObject("""
                INSERT INTO client (email, first_name, last_name, birth_date, phone)
                VALUES (?, ?, 'Client', '1985-03-02', '0600000000')
                RETURNING id
                """, Long.class, email, firstName);
        return jdbc.queryForObject("""
                INSERT INTO appointment (client_id, offering_slug, offering_name, price_cents, mode, starts_at,
                                         ends_at, occupied_until, status, reason, messenger_name, consented_at)
                VALUES (?, 'consultation-1-h', 'Consultation approfondie', 8000, 'VIDEO', ?::timestamptz,
                        ?::timestamptz + interval '1 hour', ?::timestamptz + interval '75 minutes',
                        'CONFIRMED', 'Test', 'Client', now())
                RETURNING id
                """, Long.class, clientId, start, start, start);
    }

    private MvcTestResult complete(long appointmentId) {
        return mvc.post().uri("/api/admin/appointments/{id}/complete", appointmentId)
            .with(practitioner()).with(csrf()).exchange();
    }

    private MvcTestResult billOf(long appointmentId) {
        return mvc.get().uri("/api/admin/bills?appointmentId={id}", appointmentId).with(practitioner()).exchange();
    }

    private MvcTestResult pay(Number billId, String paidOn) {
        return mvc.post().uri("/api/admin/bills/{id}/payment", billId).with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"method": "TRANSFER", "paidOn": "%s"}
                """.formatted(paidOn))
            .exchange();
    }

    /** Emails leave after the commit, in another thread: waits for the expected one, four seconds at most. */
    private SimpleMailMessage awaitMail(String to, String subjectStart) throws InterruptedException {
        for (var attempt = 0; attempt < 40; attempt++) {
            Optional<SimpleMailMessage> found = mockingDetails(mailSender).getInvocations().stream()
                .flatMap(invocation -> Arrays.stream(invocation.getArguments()))
                .filter(SimpleMailMessage.class::isInstance)
                .map(SimpleMailMessage.class::cast)
                .filter(mail -> mail.getTo() != null && Arrays.asList(mail.getTo()).contains(to))
                .filter(mail -> mail.getSubject() != null && mail.getSubject().startsWith(subjectStart))
                .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No email \"" + subjectStart + "…\" to " + to);
    }
}
