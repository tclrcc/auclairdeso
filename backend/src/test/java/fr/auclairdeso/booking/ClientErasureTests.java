package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingCategory;
import fr.auclairdeso.catalog.OfferingView;
import fr.auclairdeso.catalog.PaymentPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientErasureTests {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final String ERASED = "Effacé à la demande de la cliente.";

    @Test
    void anErasedClientKeepsNothingThatNamesHer() {
        var client = client();
        client.markTrusted(true);

        client.erase(NOW);

        var summary = client.toSummary();
        assertThat(summary.email()).isNull();
        assertThat(summary.birthDate()).isNull();
        assertThat(summary.city()).isNull();
        assertThat(summary.phone()).isEqualTo("0000000000");
        assertThat(summary.trusted()).isFalse();
        assertThat(summary.erased()).isTrue();
    }

    @Test
    void aBlockedClientKeepsHerPhoneNumberSoThatTheBlockHolds() {
        var client = client();
        client.block();

        client.erase(NOW);

        assertThat(client.toSummary().phone()).isEqualTo("06 12 34 56 78");
        assertThat(client.isBlocked()).isTrue();
        assertThat(client.isErased()).isTrue();
    }

    @Test
    void aSessionLosesWhatTheClientWroteButKeepsItsShape() {
        var appointment = videoRequest();

        appointment.anonymise();

        var view = appointment.toAdminView(PARIS, false);
        assertThat(view.reason()).isEqualTo(ERASED);
        assertThat(view.messengerName()).isEqualTo(ERASED);
        // Nothing is invented where there was nothing: this session is not at the client's home.
        assertThat(view.address()).isNull();
        assertThat(view.priceCents()).isEqualTo(8_000);
        assertThat(view.status()).isEqualTo(AppointmentStatus.REQUESTED);
    }

    private static Client client() {
        var client = new Client("alice@example.com");
        client.update(new ClientDetails("Alice", "Martin", LocalDate.of(1990, 5, 4), "06 12 34 56 78", "Lyon"));
        return client;
    }

    private static Appointment videoRequest() {
        var details = new ClientDetails("Alice", "Martin", LocalDate.of(1990, 5, 4), "06 12 34 56 78", null);
        var offering = new OfferingView("consultation-1-h", "Consultation approfondie", "Une heure.", 60, 15, 8_000,
            PaymentPolicy.ON_SITE, null, List.of(ConsultationMode.VIDEO), OfferingCategory.CLAIRVOYANCE);
        var booking = new BookingRequest("consultation-1-h", ConsultationMode.VIDEO,
            OffsetDateTime.parse("2026-10-21T14:00:00+02:00"), details, "Une question.", null, "Alice M",
            true, true);
        return Appointment.request(client(), offering, booking, NOW);
    }
}
