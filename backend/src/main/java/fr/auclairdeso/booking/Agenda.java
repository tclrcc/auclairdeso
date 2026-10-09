package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The practitioner's agenda: requests to handle, appointments to come, decisions (docs/specification.md, 8).
 */
@Service
@Transactional
class Agenda {

    private final AppointmentRepository appointments;
    private final AppointmentPhotoRepository photos;
    private final BookingRules rules;
    private final Clock clock;

    Agenda(AppointmentRepository appointments, AppointmentPhotoRepository photos, BookingRules rules, Clock clock) {
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

    AdminAppointmentView confirm(long id) {
        return change(id, Appointment::confirm);
    }

    AdminAppointmentView decline(long id) {
        return change(id, Appointment::decline);
    }

    private AdminAppointmentView change(long id, Consumer<Appointment> transition) {
        var appointment = appointments.findWithClientById(id)
            .orElseThrow(() -> new BookingRefusedException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
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

    /** Confirmed sessions that have started and still wait to be marked completed or no-show. */
    @Transactional(readOnly = true)
    List<AdminAppointmentView> toClose() {
        return views(appointments.findByStatusAndStartsAtBeforeOrderByStartsAtAsc(
            AppointmentStatus.CONFIRMED, Instant.now(clock)));
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
}
