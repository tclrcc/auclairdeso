package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The practitioner's agenda: requests to handle, appointments to come, decisions (docs/specification.md, 8).
 */
@Service
@Transactional
class Agenda {

    private final OfferingCatalog catalog;
    private final ClientRepository clients;
    private final AppointmentRepository appointments;
    private final AppointmentPhotoRepository photos;
    private final BookingRules rules;
    private final Clock clock;

    Agenda(OfferingCatalog catalog, ClientRepository clients, AppointmentRepository appointments,
           AppointmentPhotoRepository photos, BookingRules rules, Clock clock) {
        this.catalog = catalog;
        this.clients = clients;
        this.appointments = appointments;
        this.photos = photos;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    List<AdminAppointmentView> pending() {
        return views(appointments.findByStatusAndStartsAtAfterOrderByStartsAtAsc(
            AppointmentStatus.REQUESTED, Instant.now(clock)));
    }

    /** Confirmed sessions that have started and still wait to be marked completed or no-show. */
    @Transactional(readOnly = true)
    List<AdminAppointmentView> toClose() {
        return views(appointments.findByStatusAndStartsAtBeforeOrderByStartsAtAsc(
            AppointmentStatus.CONFIRMED, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    List<AdminAppointmentView> between(LocalDate from, LocalDate to) {
        var start = from.atStartOfDay(rules.zone()).toInstant();
        var end = to.plusDays(1).atStartOfDay(rules.zone()).toInstant();
        return views(appointments.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(start, end));
    }

    @Transactional(readOnly = true)
    Optional<AdminAppointmentView> find(long id) {
        return appointments.findWithClientById(id).map(this::view);
    }

    @Transactional(readOnly = true)
    Optional<AppointmentPhoto> photo(long id) {
        return photos.findById(id);
    }

    /**
     * An appointment taken by the practitioner: no planning rule applies (she decides),
     * except that it can never overlap another session, which the database enforces.
     */
    AdminAppointmentView create(ManualAppointmentRequest request) {
        var offering = catalog.findActiveOffering(request.offeringSlug())
            .orElseThrow(() -> refused(HttpStatus.BAD_REQUEST, "Cette séance n'existe pas ou est masquée."));
        if (!offering.modes().contains(request.mode())) {
            throw refused(HttpStatus.BAD_REQUEST, "Ce mode de consultation n'est pas proposé pour cette séance.");
        }
        if (request.mode() == ConsultationMode.CLIENT_HOME && isBlank(request.address())) {
            throw refused(HttpStatus.BAD_REQUEST, "Indiquez l'adresse de la séance.");
        }
        if (request.mode() == ConsultationMode.VIDEO && isBlank(request.messengerName())) {
            throw refused(HttpStatus.BAD_REQUEST, "Indiquez le nom Messenger de la cliente.");
        }
        var client = request.clientId() != null
            ? clients.findById(request.clientId())
            .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Cliente introuvable."))
            : clientFor(request.newClient());
        var start = request.start().atZone(rules.zone()).toInstant();
        try {
            return view(appointments.saveAndFlush(Appointment.byPractitioner(client, offering, request, start)));
        } catch (DataIntegrityViolationException overlap) {
            throw refused(HttpStatus.CONFLICT, "Ce créneau chevauche un autre rendez-vous, pause comprise.");
        }
    }

    AdminAppointmentView confirm(long id) {
        return change(id, Appointment::confirm);
    }

    AdminAppointmentView decline(long id) {
        return change(id, Appointment::decline);
    }

    AdminAppointmentView cancel(long id) {
        return change(id, Appointment::cancelByPractitioner);
    }

    AdminAppointmentView complete(long id) {
        return change(id, appointment -> appointment.complete(Instant.now(clock)));
    }

    AdminAppointmentView noShow(long id) {
        return change(id, appointment -> appointment.markNoShow(Instant.now(clock)));
    }

    /** Reuses the client who already has this email, if any; otherwise creates her. */
    private Client clientFor(ManualClient details) {
        var email = isBlank(details.email()) ? null : details.email().strip().toLowerCase(Locale.ROOT);
        if (email != null) {
            var existing = clients.findByEmail(email);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return clients.save(Client.byPractitioner(details, email));
    }

    private AdminAppointmentView change(long id, Consumer<Appointment> transition) {
        var appointment = appointments.findWithClientById(id)
            .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
        transition.accept(appointment);
        return view(appointment);
    }

    private List<AdminAppointmentView> views(List<Appointment> list) {
        Set<Long> withPhoto = list.isEmpty()
            ? Set.of()
            : photos.findAppointmentIdsWithPhoto(list.stream().map(Appointment::id).toList());
        return list.stream()
            .map(appointment -> appointment.toAdminView(rules.zone(), withPhoto.contains(appointment.id())))
            .toList();
    }

    private AdminAppointmentView view(Appointment appointment) {
        return appointment.toAdminView(rules.zone(), photos.existsById(appointment.id()));
    }

    private static BookingRefusedException refused(HttpStatus status, String detail) {
        return new BookingRefusedException(status, detail);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
