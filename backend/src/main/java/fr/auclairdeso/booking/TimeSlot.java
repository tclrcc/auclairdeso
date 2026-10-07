package fr.auclairdeso.booking;

import java.time.OffsetDateTime;

/** A slot a client can book. Times carry their offset, e.g. 2026-10-12T14:00+02:00. */
record TimeSlot(OffsetDateTime start, OffsetDateTime end) {
}
