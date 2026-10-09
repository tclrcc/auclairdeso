package fr.auclairdeso.booking;

/** How an appointment was created. */
enum AppointmentSource {

    /** Requested by the client on the site. */
    ONLINE,

    /** Entered by the practitioner, e.g. after a phone call. */
    PRACTITIONER
}
