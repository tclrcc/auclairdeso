package fr.auclairdeso.notification;

import fr.auclairdeso.billing.BillIssued;
import fr.auclairdeso.booking.AppointmentEvent;
import fr.auclairdeso.booking.AppointmentNotice;
import fr.auclairdeso.booking.EveningProgram;
import fr.auclairdeso.identity.StaffDirectory;
import fr.auclairdeso.shared.AppProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Emails sent when an appointment changes, and the bill of each completed session (docs/specification.md, 7).
 * <p>
 * An {@code @ApplicationModuleListener} runs after the commit, in another thread: a slow mail server
 * never delays the page. If sending fails, the event stays in the event_publication table and is
 * sent again later (FailedNotificationRetries).
 */
@Component
class BookingNotifications {

    private final JavaMailSender mailSender;
    private final StaffDirectory staff;
    private final AppProperties app;
    private final Messages messages;

    BookingNotifications(JavaMailSender mailSender, StaffDirectory staff, AppProperties app,
                         NotificationProperties properties) {
        this.mailSender = mailSender;
        this.staff = staff;
        this.app = app;
        this.messages = new Messages(app.frontendUrl(), properties);
    }

    @ApplicationModuleListener
    void on(AppointmentEvent event) {
        switch (event) {
            case AppointmentEvent.Requested(var appointment) -> toPractitioners(messages.newRequest(appointment));
            case AppointmentEvent.Confirmed(var appointment) -> toClient(appointment, messages.confirmed(appointment));
            case AppointmentEvent.Declined(var appointment) -> toClient(appointment, messages.declined(appointment));
            case AppointmentEvent.CancelledByPractitioner(var appointment) ->
                toClient(appointment, messages.cancelledByPractitioner(appointment));
            case AppointmentEvent.CancelledByClient(var appointment, var wasConfirmed, var late) -> {
                toClient(appointment, messages.cancelledByClient(appointment, wasConfirmed, late));
                toPractitioners(messages.clientCancelled(appointment, wasConfirmed, late));
            }
            case AppointmentEvent.Expired(var appointment) -> toClient(appointment, messages.expired(appointment));
            case AppointmentEvent.ReminderDue(var appointment) -> toClient(appointment, messages.reminder(appointment));
            // The email of a completed session is its bill: see on(BillIssued).
            case AppointmentEvent.Completed _ -> {
            }
        }
    }

    /** The bill of a completed session (arrêté 83-50/A). A client without email gets it on paper. */
    @ApplicationModuleListener
    void on(BillIssued issued) {
        var bill = issued.bill();
        if (bill.clientEmail() != null) {
            send(bill.clientEmail(), messages.bill(bill));
        }
    }

    @ApplicationModuleListener
    void on(EveningProgram program) {
        toPractitioners(messages.eveningProgram(program));
    }

    /** A client entered by the practitioner may have no email: then she tells her herself. */
    private void toClient(AppointmentNotice appointment, Message message) {
        var email = appointment.clientEmail();
        if (email != null) {
            send(email, message);
        }
    }

    private void toPractitioners(Message message) {
        staff.practitionerEmails().forEach(email -> send(email, message));
    }

    private void send(String to, Message message) {
        var mail = new SimpleMailMessage();
        mail.setFrom(app.mailFrom());
        mail.setTo(to);
        mail.setSubject(message.subject());
        mail.setText(message.text());
        mailSender.send(mail);
    }
}
