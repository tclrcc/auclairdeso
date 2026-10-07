package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.OfferingCatalog;
import fr.auclairdeso.catalog.OfferingView;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gathers the schedule, the closures and the booked sessions, then lets the calculator decide.
 */
@Service
@Transactional(readOnly = true)
class Availability {

    private final OfferingCatalog catalog;
    private final OpeningHoursRepository openingHours;
    private final ClosureRepository closures;
    private final AppointmentRepository appointments;
    private final SlotCalculator calculator;
    private final BookingRules rules;

    Availability(OfferingCatalog catalog, OpeningHoursRepository openingHours, ClosureRepository closures,
                 AppointmentRepository appointments, SlotCalculator calculator, BookingRules rules) {
        this.catalog = catalog;
        this.openingHours = openingHours;
        this.closures = closures;
        this.appointments = appointments;
        this.calculator = calculator;
        this.rules = rules;
    }

    Optional<List<TimeSlot>> forOffering(String slug, LocalDate from, LocalDate to) {
        return catalog.findActiveOffering(slug).map(offering -> slots(offering, from, to));
    }

    /** Whether this exact start is one of the slots offered right now. */
    boolean isAvailable(OfferingView offering, OffsetDateTime start) {
        var day = start.atZoneSameInstant(rules.zone()).toLocalDate();
        return slots(offering, day, day).stream()
            .anyMatch(slot -> slot.start().toInstant().equals(start.toInstant()));
    }

    private List<TimeSlot> slots(OfferingView offering, LocalDate from, LocalDate to) {
        return calculator.availableSlots(
            from, to,
            Duration.ofMinutes(offering.durationMinutes()),
            Duration.ofMinutes(offering.bufferMinutes()),
            openingHours.findAll().stream().map(OpeningHours::toPeriod).toList(),
            closures.findByLastDayGreaterThanEqualAndFirstDayLessThanEqual(from, to).stream()
                .map(Closure::toPeriod)
                .toList(),
            bookedSessions(from, to));
    }

    private List<BookedSession> bookedSessions(LocalDate from, LocalDate to) {
        var start = from.atStartOfDay(rules.zone()).toInstant();
        var end = to.plusDays(1).atStartOfDay(rules.zone()).toInstant();
        return appointments.findByStatusInAndStartsAtBeforeAndOccupiedUntilAfter(
                AppointmentStatus.HOLDING_SLOT, end, start).stream()
            .map(appointment -> appointment.toBookedSession(rules.zone()))
            .toList();
    }
}
