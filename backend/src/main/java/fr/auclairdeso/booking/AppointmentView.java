package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import java.time.OffsetDateTime;

record AppointmentView(Long id, String offeringSlug, String offeringName, ConsultationMode mode,
                       OffsetDateTime start, OffsetDateTime end, AppointmentStatus status, int priceCents) {
}
