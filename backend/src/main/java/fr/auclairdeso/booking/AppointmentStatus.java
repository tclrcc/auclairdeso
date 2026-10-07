package fr.auclairdeso.booking;

import java.util.EnumSet;
import java.util.Set;

/** Life cycle of an appointment (docs/specification.md, 5.5). */
enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    DECLINED,
    EXPIRED,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    /** Statuses whose appointment keeps its slot, as in the appointment_no_overlap constraint. */
    static final Set<AppointmentStatus> HOLDING_SLOT = EnumSet.of(REQUESTED, CONFIRMED);
}
