package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * What the other modules may know about an appointment, at the moment of an event.
 * Times are local times of the practitioner's time zone (Europe/Paris).
 *
 * @param clientEmail null for a client entered by the practitioner without an email
 * @param freeCancellationUntil until when the client may cancel without it counting as late
 * @param address where the session takes place, for a session at the client's home
 * @param messengerName the client's name on Messenger, for a video session
 */
public record AppointmentNotice(
    long id,
    @Nullable String clientEmail,
    String clientFirstName,
    String clientLastName,
    String clientPhone,
    String offeringName,
    ConsultationMode mode,
    LocalDateTime start,
    LocalDateTime freeCancellationUntil,
    int priceCents,
    @Nullable String address,
    @Nullable String messengerName) {
}
