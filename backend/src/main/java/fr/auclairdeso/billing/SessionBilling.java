package fr.auclairdeso.billing;

import fr.auclairdeso.booking.AppointmentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Issues the bill as soon as the practitioner marks a session completed: the client must receive it
 * before she pays (arrêté 83-50/A).
 * <p>
 * A plain {@code @EventListener} runs at once, in the transaction of the completion: if the bill cannot be
 * issued, the completion is cancelled too, and when the page reloads the bill is already there. The email
 * that carries the bill, on the other hand, leaves after the commit (notification module).
 */
@Component
class SessionBilling {

    private final Bills bills;

    SessionBilling(Bills bills) {
        this.bills = bills;
    }

    @EventListener
    void on(AppointmentEvent.Completed completed) {
        bills.issue(completed.appointment());
    }
}
