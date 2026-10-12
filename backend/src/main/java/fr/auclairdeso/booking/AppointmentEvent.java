package fr.auclairdeso.booking;

/**
 * What happens to an appointment, told to the other modules (notifications, billing).
 * <p>
 * Published inside the transaction of the change. Spring Modulith hands them to the
 * {@code @ApplicationModuleListener}s after the commit, and keeps them in the event_publication table
 * until each listener succeeded. A plain {@code @EventListener} runs at once, inside the transaction:
 * billing issues the bill of a completed session that way.
 */
public sealed interface AppointmentEvent {

    AppointmentNotice appointment();

    /** A client sent a booking request: the practitioner has to answer it. */
    record Requested(AppointmentNotice appointment) implements AppointmentEvent {
    }

    /** The practitioner accepted a request, or entered an appointment herself. */
    record Confirmed(AppointmentNotice appointment) implements AppointmentEvent {
    }

    record Declined(AppointmentNotice appointment) implements AppointmentEvent {
    }

    /** The practitioner cancelled: she also calls the client (docs/specification.md, 6). */
    record CancelledByPractitioner(AppointmentNotice appointment) implements AppointmentEvent {
    }

    /**
     * @param wasConfirmed false when the client withdrew a request still pending
     * @param late whether the cancellation counts as late
     */
    record CancelledByClient(AppointmentNotice appointment, boolean wasConfirmed, boolean late)
        implements AppointmentEvent {
    }

    /** Nobody answered the request in time: its slot is free again. */
    record Expired(AppointmentNotice appointment) implements AppointmentEvent {
    }

    /** The day before a confirmed session. */
    record ReminderDue(AppointmentNotice appointment) implements AppointmentEvent {
    }

    /** The practitioner marked the session completed: it can be billed. */
    record Completed(AppointmentNotice appointment) implements AppointmentEvent {
    }
}
