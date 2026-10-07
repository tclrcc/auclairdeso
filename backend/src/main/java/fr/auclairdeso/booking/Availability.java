package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.OfferingCatalog;
import java.time.Duration;
import java.time.LocalDate;
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
    private final SlotCalculator calculator;

    Availability(OfferingCatalog catalog, OpeningHoursRepository openingHours,
                 ClosureRepository closures, SlotCalculator calculator) {
        this.catalog = catalog;
        this.openingHours = openingHours;
        this.closures = closures;
        this.calculator = calculator;
    }

    Optional<List<TimeSlot>> forOffering(String slug, LocalDate from, LocalDate to) {
        return catalog.findActiveOffering(slug).map(offering -> calculator.availableSlots(
            from, to,
            Duration.ofMinutes(offering.durationMinutes()),
            Duration.ofMinutes(offering.bufferMinutes()),
            openingHours.findAll().stream().map(OpeningHours::toPeriod).toList(),
            closures.findByLastDayGreaterThanEqualAndFirstDayLessThanEqual(from, to).stream()
                .map(Closure::toPeriod)
                .toList(),
            bookedSessions(from, to)));
    }

    /** Sessions already holding a slot. Appointments arrive with step 3B. */
    private List<BookedSession> bookedSessions(LocalDate from, LocalDate to) {
        return List.of();
    }
}
