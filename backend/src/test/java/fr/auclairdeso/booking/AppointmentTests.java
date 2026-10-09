package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingCategory;
import fr.auclairdeso.catalog.OfferingView;
import fr.auclairdeso.catalog.PaymentPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppointmentTests {

    /** The session starts on Monday 12 October 2026 at 14:00 in Paris, 12:00 UTC. */
    private static final Instant START = Instant.parse("2026-10-12T12:00:00Z");
    private static final Duration NOTICE = Duration.ofHours(48);

    @Test
    void aRequestCanBeConfirmedOnlyOnce() {
        var appointment = request();

        appointment.confirm();

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(appointment::confirm);
    }

    @Test
    void aConfirmedSessionCannotBeDeclinedAnymore() {
        var appointment = confirmed();

        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(appointment::decline);
    }

    @Test
    void aDeclinedRequestNoLongerHoldsItsSlot() {
        var appointment = request();

        appointment.decline();

        assertThat(AppointmentStatus.HOLDING_SLOT).doesNotContain(appointment.status());
    }

    @Test
    void cancellingAConfirmedSessionLessThanTwoDaysAheadIsLate() {
        var appointment = confirmed();

        assertThat(appointment.cancelByClient(START.minus(Duration.ofHours(28)), NOTICE)).isTrue();
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancellingEarlierIsNotLate() {
        assertThat(confirmed().cancelByClient(START.minus(Duration.ofHours(72)), NOTICE)).isFalse();
    }

    @Test
    void withdrawingAPendingRequestIsNeverLate() {
        assertThat(request().cancelByClient(START.minus(Duration.ofHours(1)), NOTICE)).isFalse();
    }

    @Test
    void aSessionCanOnlyBeClosedOnceStarted() {
        var appointment = confirmed();

        assertThatExceptionOfType(BookingRefusedException.class)
            .isThrownBy(() -> appointment.complete(START.minus(Duration.ofMinutes(5))));

        appointment.complete(START.plus(Duration.ofMinutes(5)));
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void aNoShowBlocksTheClient() {
        var appointment = confirmed();

        appointment.markNoShow(START.plus(Duration.ofHours(1)));

        assertThat(appointment.client().isBlocked()).isTrue();
    }

    private static Appointment confirmed() {
        var appointment = request();
        appointment.confirm();
        return appointment;
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
        return Appointment.request(client, offering, booking, Instant.parse("2026-10-01T08:00:00Z"));
    }
}
