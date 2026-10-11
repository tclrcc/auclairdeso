package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import java.time.OffsetDateTime;

/** One appointment in the history of a client file. */
record ClientHistoryEntry(Long id, OffsetDateTime start, String offeringName, ConsultationMode mode,
                          AppointmentStatus status, int priceCents, boolean lateCancellation) {
}
