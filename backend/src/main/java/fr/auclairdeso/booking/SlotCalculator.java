package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Computes the slots a client can book for one offering (docs/specification.md, 4.2).
 * Pure logic, without database or Spring, so that every rule can be tested in isolation.
 */
final class SlotCalculator {

    private final BookingRules rules;
    private final Clock clock;

    SlotCalculator(BookingRules rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    List<TimeSlot> availableSlots(LocalDate from, LocalDate to, Duration duration, Duration breakAfter,
                                  List<OpeningPeriod> openings, List<ClosedPeriod> closures,
                                  List<BookedSession> booked) {
        var today = LocalDate.now(clock.withZone(rules.zone()));
        var firstDay = latest(from, today.plusDays(rules.minDaysAhead()));
        var lastDay = earliest(to, today.plus(rules.horizon()));

        var slots = new ArrayList<TimeSlot>();
        for (var day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            if (isClosed(day, closures) || sessionsOn(day, booked) >= rules.maxSessionsPerDay()) {
                continue;
            }
            for (var opening : openingsOn(day, openings)) {
                slots.addAll(chainedSlots(day, opening, duration, breakAfter, booked));
            }
        }
        return slots;
    }

    /**
     * Chains the slots of one opening period: each slot starts when the previous one and its break
     * are over. A session already booked on the way pushes the chain right after its own break.
     * A session must end before closing time; its break may go beyond.
     */
    private List<TimeSlot> chainedSlots(LocalDate day, OpeningPeriod opening, Duration duration,
                                        Duration breakAfter, List<BookedSession> booked) {
        var slots = new ArrayList<TimeSlot>();
        var closing = day.atTime(opening.end()).atZone(rules.zone());
        var start = day.atTime(opening.start()).atZone(rules.zone());
        while (!start.plus(duration).isAfter(closing)) {
            var occupiedUntil = start.plus(duration).plus(breakAfter);
            var blockedUntil = endOfOverlappingSessions(start, occupiedUntil, booked);
            if (blockedUntil.isPresent()) {
                start = blockedUntil.get().withZoneSameInstant(rules.zone());
            } else {
                slots.add(new TimeSlot(start.toOffsetDateTime(), start.plus(duration).toOffsetDateTime()));
                start = occupiedUntil;
            }
        }
        return slots;
    }

    /** When the latest of the booked sessions overlapping [from, until) is over, break included. */
    private static Optional<ZonedDateTime> endOfOverlappingSessions(ZonedDateTime from, ZonedDateTime until,
                                                                    List<BookedSession> booked) {
        return booked.stream()
            .filter(session -> session.start().isBefore(until) && from.isBefore(session.occupiedUntil()))
            .map(BookedSession::occupiedUntil)
            .max(ChronoZonedDateTime.timeLineOrder());
    }

    private static boolean isClosed(LocalDate day, List<ClosedPeriod> closures) {
        return closures.stream().anyMatch(closure -> closure.covers(day));
    }

    private long sessionsOn(LocalDate day, List<BookedSession> booked) {
        return booked.stream()
            .filter(session -> session.start().withZoneSameInstant(rules.zone()).toLocalDate().equals(day))
            .count();
    }

    private static List<OpeningPeriod> openingsOn(LocalDate day, List<OpeningPeriod> openings) {
        return openings.stream()
            .filter(opening -> opening.day() == day.getDayOfWeek())
            .sorted(Comparator.comparing(OpeningPeriod::start))
            .toList();
    }

    private static LocalDate latest(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
