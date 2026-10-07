package fr.auclairdeso.booking;

import java.time.LocalDate;

/** Whole days without any session, both ends included. */
record ClosedPeriod(LocalDate firstDay, LocalDate lastDay) {

    boolean covers(LocalDate day) {
        return !day.isBefore(firstDay) && !day.isAfter(lastDay);
    }
}
