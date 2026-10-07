package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingCategory;
import fr.auclairdeso.catalog.OfferingView;
import fr.auclairdeso.catalog.PaymentPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppointmentTests {

    @Test
    void aRequestCanBeConfirmedOnlyOnce() {
        var appointment = request();

        appointment.confirm();

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(appointment::confirm);
    }

    @Test
    void aConfirmedSessionCannotBeDeclinedAnymore() {
        var appointment = request();
        appointment.confirm();

        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(appointment::decline);
    }

    @Test
    void aDeclinedRequestNoLongerHoldsItsSlot() {
        var appointment = request();

        appointment.decline();

        assertThat(AppointmentStatus.HOLDING_SLOT).doesNotContain(appointment.status());
    }

    private static Appointment request() {
        var details = new ClientDetails("Alice", "Martin", LocalDate.of(1990, 5, 4), "06 12 34 56 78", null);
        var client = new Client("alice@example.com");
        client.update(details);
        var offering = new OfferingView("consultation-1-h", "Consultation approfondie", "Une heure.", 60, 15, 8_000,
            PaymentPolicy.ON_SITE, null, List.of(ConsultationMode.VIDEO), OfferingCategory.CLAIRVOYANCE);
        var booking = new BookingRequest("consultation-1-h", ConsultationMode.VIDEO,
            OffsetDateTime.parse("2026-10-12T14:00:00+02:00"), details, "Une question.", null, "Alice M",
            true, true);
        return Appointment.request(client, offering, booking, Instant.parse("2026-10-11T08:00:00Z"));
    }
}
