package fr.auclairdeso.booking;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SlotCalculatorTests {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final BookingRules RULES = new BookingRules(PARIS, 3, 1, Period.ofMonths(2));

    /** Sunday 11 October 2026, 10:00 in Paris. */
    private static final Clock SUNDAY = Clock.fixed(Instant.parse("2026-10-11T08:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate MONDAY_12 = LocalDate.of(2026, 10, 12);
    private static final LocalDate TUESDAY_13 = LocalDate.of(2026, 10, 13);

    private static final List<OpeningPeriod> MONDAY_TO_THURSDAY = Stream.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY)
        .map(day -> new OpeningPeriod(day, LocalTime.of(14, 0), LocalTime.of(18, 0)))
        .toList();

    private final SlotCalculator calculator = new SlotCalculator(RULES, SUNDAY);

    @Test
    void hourLongSessionsChainWithAQuarterHourBreak() {
        assertThat(startsOn(MONDAY_12, 60, 15)).containsExactly(at(14, 0), at(15, 15), at(16, 30));
    }

    @Test
    void halfHourSessionsChainWithAHalfHourBreak() {
        assertThat(startsOn(MONDAY_12, 30, 30)).containsExactly(at(14, 0), at(15, 0), at(16, 0), at(17, 0));
    }

    @Test
    void nothingOnDaysWithoutOpeningHours() {
        assertThat(startsOn(LocalDate.of(2026, 10, 16), 60, 15)).isEmpty();
    }

    @Test
    void todayIsNeverBookableButTomorrowIs() {
        var mondayMorning = new SlotCalculator(RULES, Clock.fixed(Instant.parse("2026-10-12T07:00:00Z"), ZoneOffset.UTC));

        assertThat(slots(mondayMorning, MONDAY_12, 60, 15)).isEmpty();
        assertThat(slots(mondayMorning, TUESDAY_13, 60, 15)).hasSize(3);
    }

    @Test
    void nothingBeyondTwoMonths() {
        // Today is Sunday 11 October: Thursday 10 December is bookable, Monday 14 December is not.
        assertThat(startsOn(LocalDate.of(2026, 12, 10), 60, 15)).hasSize(3);
        assertThat(startsOn(LocalDate.of(2026, 12, 14), 60, 15)).isEmpty();
    }

    @Test
    void closedDaysOfferNothing() {
        var slots = calculator.availableSlots(MONDAY_12, TUESDAY_13, Duration.ofHours(1), Duration.ofMinutes(15),
            MONDAY_TO_THURSDAY, List.of(new ClosedPeriod(MONDAY_12, MONDAY_12)), List.of());

        assertThat(slots).extracting(slot -> slot.start().toLocalDate()).containsOnly(TUESDAY_13);
    }

    @Test
    void theChainResumesAfterABookedSessionAndItsBreak() {
        // A 30-minute session at 14:00 keeps the practitioner busy until 15:00.
        assertThat(startsOn(MONDAY_12, 60, 15, session(MONDAY_12, 14, 0, 30, 30)))
            .containsExactly(at(15, 0), at(16, 15));
    }

    @Test
    void aSlotMayEndRightWhenTheNextSessionStarts() {
        // 15:15 + 1 h + 15 min break = 16:30, exactly when the booked session starts.
        assertThat(startsOn(MONDAY_12, 60, 15, session(MONDAY_12, 16, 30, 60, 15)))
            .containsExactly(at(14, 0), at(15, 15));
    }

    @Test
    void aFullDayOffersNothing() {
        assertThat(startsOn(MONDAY_12, 30, 30,
            session(MONDAY_12, 14, 0, 30, 30),
            session(MONDAY_12, 15, 0, 30, 30),
            session(MONDAY_12, 16, 0, 30, 30)))
            .isEmpty();
    }

    private List<LocalTime> startsOn(LocalDate day, int durationMinutes, int breakMinutes, BookedSession... booked) {
        return slots(calculator, day, durationMinutes, breakMinutes, booked).stream()
            .map(slot -> slot.start().toLocalTime())
            .toList();
    }

    private static List<TimeSlot> slots(SlotCalculator calculator, LocalDate day, int durationMinutes,
                                        int breakMinutes, BookedSession... booked) {
        return calculator.availableSlots(day, day, Duration.ofMinutes(durationMinutes),
            Duration.ofMinutes(breakMinutes), MONDAY_TO_THURSDAY, List.of(), List.of(booked));
    }

    private static BookedSession session(LocalDate day, int hour, int minute, int durationMinutes, int breakMinutes) {
        return new BookedSession(day.atTime(hour, minute).atZone(PARIS),
            Duration.ofMinutes(durationMinutes), Duration.ofMinutes(breakMinutes));
    }

    private static LocalTime at(int hour, int minute) {
        return LocalTime.of(hour, minute);
    }
}
