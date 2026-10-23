package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The right to erasure (docs/specification.md, 9): the client erases her account herself.
 * <p>
 * Her sessions stay, and so do their bills, because accounting records are kept ten years. Nothing of
 * her is left in the booking module: no name, no email, no birth date, no reason, no portrait, no
 * private note. The bills keep the name that was printed on them, which the arrêté 83-50/A requires.
 */
@Service
@Transactional
class ClientErasure {

    private final ClientRepository clients;
    private final ClientNoteRepository notes;
    private final AppointmentRepository appointments;
    private final AppointmentPhotoRepository photos;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ClientErasure(ClientRepository clients, ClientNoteRepository notes, AppointmentRepository appointments,
                  AppointmentPhotoRepository photos, ApplicationEventPublisher events, Clock clock) {
        this.clients = clients;
        this.notes = notes;
        this.appointments = appointments;
        this.photos = photos;
        this.events = events;
        this.clock = clock;
    }

    ErasureReport erase(String email) {
        var now = Instant.now(clock);
        var client = clients.findByEmail(email).orElse(null);
        var report = client == null ? new ErasureReport(0, 0, false) : erase(client, now);
        // Even a client who never booked has a login to delete.
        events.publishEvent(new ClientErased(email));
        return report;
    }

    private ErasureReport erase(Client client, Instant now) {
        if (appointments.existsByClientAndStatusInAndStartsAtAfter(client, AppointmentStatus.HOLDING_SLOT, now)) {
            throw new BookingRefusedException(HttpStatus.CONFLICT,
                "Annulez d'abord vos rendez-vous à venir, puis revenez supprimer votre compte.");
        }
        var past = appointments.findByClientOrderByStartsAtDesc(client);
        past.forEach(Appointment::anonymise);
        var deletedPhotos = past.isEmpty()
            ? 0
            : photos.deleteByAppointmentIdIn(past.stream().map(Appointment::id).toList());
        notes.deleteByClient(client);
        var phoneKept = client.isBlocked();
        client.erase(now);
        return new ErasureReport(past.size(), deletedPhotos, phoneKept);
    }
}
