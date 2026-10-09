package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import java.time.OffsetDateTime;

/**
 * An appointment as the client sees it.
 *
 * @param cancellationDeadline after this moment, cancelling a confirmed session counts as late
 */
record AppointmentView(Long id, String offeringSlug, String offeringName, ConsultationMode mode,
                       OffsetDateTime start, OffsetDateTime end, AppointmentStatus status, int priceCents,
                       OffsetDateTime cancellationDeadline, boolean lateCancellation) {
}
