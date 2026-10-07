package fr.auclairdeso.booking;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/** A weekly opening period, e.g. Monday from 14:00 to 18:00, in the practitioner's time zone. */
record OpeningPeriod(DayOfWeek day, LocalTime start, LocalTime end) {

    OpeningPeriod {
        Objects.requireNonNull(day, "day");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("An opening period must start before it ends");
        }
    }
}
