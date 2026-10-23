package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What time does to the appointments (docs/specification.md, 5.5, 7 and 9): unanswered requests expire,
 * reminders leave the day before, the practitioner receives her program every evening, and the portraits
 * are deleted once their session is old enough.
 * <p>
 * Each method finds by itself what is due at the time of the clock: calling it twice, or late after
 * a restart, never sends anything twice and never misses anything.
 */
@Service
@Transactional
class AppointmentDeadlines {

    private final AppointmentRepository appointments;
    private final AppointmentPhotoRepository photos;
    private final BookingRules rules;
    private final BookingDeadlines deadlines;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    AppointmentDeadlines(AppointmentRepository appointments, AppointmentPhotoRepository photos, BookingRules rules,
                         BookingDeadlines deadlines, ApplicationEventPublisher events, Clock clock) {
        this.appointments = appointments;
        this.photos = photos;
        this.rules = rules;
        this.deadlines = deadlines;
        this.events = events;
        this.clock = clock;
    }

    /**
     * A portrait is only needed for its session: once it is old enough, it is deleted, whatever happened
     * to the appointment (docs/specification.md, 9). The appointment itself stays.
     *
     * @return how many portraits were deleted
     */
    int deleteExpiredPhotos() {
        return photos.deleteBySessionEndedBefore(Instant.now(clock).minus(rules.photoRetention()));
    }

    /**
     * A request still unanswered the day before its session, at the deadline, expires: its slot is free
     * again and the client is told. Before the deadline, only requests of today or of the past expire.
     *
     * @return how many requests expired
     */
    int expireUnansweredRequests() {
        var now = Instant.now(clock).atZone(rules.zone());
        var lastExpiringDay = now.toLocalTime().isBefore(deadlines.requestDeadline())
            ? now.toLocalDate()
            : now.toLocalDate().plusDays(1);
        var expiring = appointments.findByStatusAndStartsAtBeforeOrderByStartsAtAsc(
            AppointmentStatus.REQUESTED, startOf(lastExpiringDay.plusDays(1)));
        for (var appointment : expiring) {
            appointment.expire();
            events.publishEvent(new AppointmentEvent.Expired(notice(appointment)));
        }
        return expiring.size();
    }

    /**
     * From the reminder time, each confirmed session of tomorrow gets its reminder, once.
     *
     * @return how many reminders were sent
     */
    int sendReminders() {
        var now = Instant.now(clock).atZone(rules.zone());
        if (now.toLocalTime().isBefore(deadlines.reminderTime())) {
            return 0;
        }
        var tomorrow = now.toLocalDate().plusDays(1);
        var due = appointments.findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanAndReminderSentAtIsNull(
            AppointmentStatus.CONFIRMED, startOf(tomorrow), startOf(tomorrow.plusDays(1)));
        for (var appointment : due) {
            appointment.markReminderSent(now.toInstant());
            events.publishEvent(new AppointmentEvent.ReminderDue(notice(appointment)));
        }
        return due.size();
    }

    /**
     * Tomorrow's confirmed sessions and all the requests still waiting, for the practitioner.
     * Nothing is sent on an evening with nothing to say.
     *
     * @return whether a program was published
     */
    boolean publishEveningProgram() {
        var now = Instant.now(clock);
        var tomorrow = now.atZone(rules.zone()).toLocalDate().plusDays(1);
        var sessions = appointments
            .findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                startOf(tomorrow), startOf(tomorrow.plusDays(1)))
            .stream()
            .filter(appointment -> appointment.status() == AppointmentStatus.CONFIRMED)
            .map(this::notice)
            .toList();
        var pending = appointments
            .findByStatusAndStartsAtAfterOrderByStartsAtAsc(AppointmentStatus.REQUESTED, now)
            .stream()
            .map(this::notice)
            .toList();
        if (sessions.isEmpty() && pending.isEmpty()) {
            return false;
        }
        events.publishEvent(new EveningProgram(tomorrow, sessions, pending));
        return true;
    }

    private Instant startOf(LocalDate day) {
        return day.atStartOfDay(rules.zone()).toInstant();
    }

    private AppointmentNotice notice(Appointment appointment) {
        return appointment.toNotice(rules.zone(), rules.cancellationNotice());
    }
}
