package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** An appointment as the practitioner sees it: with the client, the reason and the details of the mode. */
record AdminAppointmentView(
    Long id,
    String offeringSlug,
    String offeringName,
    ConsultationMode mode,
    OffsetDateTime start,
    OffsetDateTime end,
    AppointmentStatus status,
    int priceCents,
    String reason,
    @Nullable String address,
    @Nullable String messengerName,
    boolean hasPhoto,
    ClientSummary client) {
}
