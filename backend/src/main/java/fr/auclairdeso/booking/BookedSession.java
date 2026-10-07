package fr.auclairdeso.booking;

import java.time.Duration;
import java.time.ZonedDateTime;

/** A session already holding a slot, followed by the break of its offering. */
record BookedSession(ZonedDateTime start, Duration duration, Duration breakAfter) {

    ZonedDateTime occupiedUntil() {
        return start.plus(duration).plus(breakAfter);
    }
}
