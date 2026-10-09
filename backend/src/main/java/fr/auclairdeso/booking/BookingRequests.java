package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking requests sent by clients (docs/specification.md, 5).
 */
@Service
class BookingRequests {

    private static final String SLOT_TAKEN = "Ce créneau n'est plus disponible. Merci d'en choisir un autre.";

    private final OfferingCatalog catalog;
    private final Availability availability;
    private final ClientRepository clients;
    private final AppointmentRepository appointments;
    private final AppointmentPhotoRepository photos;
    private final BookingRules rules;
    private final Clock clock;

    BookingRequests(OfferingCatalog catalog, Availability availability, ClientRepository clients,
                    AppointmentRepository appointments, AppointmentPhotoRepository photos,
                    BookingRules rules, Clock clock) {
        this.catalog = catalog;
        this.availability = availability;
        this.clients = clients;
        this.appointments = appointments;
        this.photos = photos;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional
    AppointmentView request(String email, BookingRequest request, @Nullable Photo photo) {
        var offering = catalog.findActiveOffering(request.offeringSlug())
            .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Cette séance n'est pas proposée."));
        var now = Instant.now(clock);

        if (!request.client().isAdultOn(LocalDate.ofInstant(now, rules.zone()))) {
            throw refused(HttpStatus.BAD_REQUEST, "Les séances sont réservées aux personnes majeures.");
        }
        if (!offering.modes().contains(request.mode())) {
            throw refused(HttpStatus.BAD_REQUEST, "Ce mode de consultation n'est pas proposé pour cette séance.");
        }
        if (request.mode() == ConsultationMode.CLIENT_HOME && isBlank(request.address())) {
            throw refused(HttpStatus.BAD_REQUEST, "Indiquez l'adresse de la séance à domicile.");
        }
        if (request.mode() == ConsultationMode.VIDEO && isBlank(request.messengerName())) {
            throw refused(HttpStatus.BAD_REQUEST, "Indiquez votre nom sur Messenger pour la séance en visio.");
        }
        if (offering.category().requiresPhoto() && photo == null) {
            throw refused(HttpStatus.BAD_REQUEST, "Une photo portrait est nécessaire pour une séance de voyance.");
        }

        var client = clients.findByEmail(email).orElseGet(() -> new Client(email));
        if (client.isBlocked()) {
            throw refused(HttpStatus.FORBIDDEN, "Merci de contacter directement Au clair de So pour prendre rendez-vous.");
        }
        if (request.mode() == ConsultationMode.IN_PERSON && !client.isTrusted()) {
            throw refused(HttpStatus.FORBIDDEN, "Les séances en présentiel sont réservées aux personnes déjà suivies.");
        }
        if (client.id() != null
            && appointments.existsByClientAndStatusInAndStartsAtAfter(client, AppointmentStatus.HOLDING_SLOT, now)) {
            throw refused(HttpStatus.CONFLICT,
                "Vous avez déjà une réservation à venir. Pour en changer, annulez-la d'abord.");
        }
        if (!availability.isAvailable(offering, request.start())) {
            throw refused(HttpStatus.CONFLICT, SLOT_TAKEN);
        }

        client.update(request.client());
        clients.save(client);
        Appointment appointment;
        try {
            appointment = appointments.saveAndFlush(Appointment.request(client, offering, request, now));
        } catch (DataIntegrityViolationException raceLost) {
            // Another request took the slot between our check and this insert: the database said no.
            throw refused(HttpStatus.CONFLICT, SLOT_TAKEN);
        }
        if (photo != null) {
            photos.save(new AppointmentPhoto(appointment.id(), photo));
        }
        return appointment.toView(rules.zone(), rules.cancellationNotice());
    }

    @Transactional(readOnly = true)
    List<AppointmentView> mine(String email) {
        return appointments.findByClientEmailOrderByStartsAtDesc(email).stream()
            .map(appointment -> appointment.toView(rules.zone(), rules.cancellationNotice()))
            .toList();
    }

    @Transactional
    AppointmentView cancel(String email, long id) {
        var appointment = appointments.findByIdAndClientEmail(id, email)
            .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
        var late = appointment.cancelByClient(Instant.now(clock), rules.cancellationNotice());
        if (late && appointments.countByClientAndLateCancellationTrue(appointment.client())
            >= rules.lateCancellationsBeforeBlock()) {
            appointment.client().block();
        }
        return appointment.toView(rules.zone(), rules.cancellationNotice());
    }

    @Transactional(readOnly = true)
    Optional<ClientProfile> profile(String email) {
        return clients.findByEmail(email).map(Client::toProfile);
    }

    private static BookingRefusedException refused(HttpStatus status, String detail) {
        return new BookingRefusedException(status, detail);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
