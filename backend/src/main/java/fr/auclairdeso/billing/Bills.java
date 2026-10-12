package fr.auclairdeso.billing;

import fr.auclairdeso.booking.AppointmentNotice;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bills of completed sessions, their payment, and the receipt book (docs/specification.md, 3.5 and 10). */
@Service
@Transactional
class Bills {

    private final BillRepository bills;
    private final BillNumbers numbers;
    private final BillingProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Bills(BillRepository bills, BillNumbers numbers, BillingProperties properties, ApplicationEventPublisher events,
          Clock clock) {
        this.bills = bills;
        this.numbers = numbers;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * The bill of a session just completed: numbered, kept, and sent to the client. A free session needs none.
     */
    void issue(AppointmentNotice session) {
        if (session.priceCents() <= 0 || bills.existsByAppointmentId(session.id())) {
            return;
        }
        var today = today();
        var bill = bills.save(Bill.issue(numbers.next(today.getYear()), today, session, properties));
        events.publishEvent(new BillIssued(bill.toNotice(properties.zone(), session.clientFirstName())));
    }

    /** The bill of a session, if it has one: an empty list otherwise. */
    @Transactional(readOnly = true)
    List<BillView> ofAppointment(long appointmentId) {
        return views(bills.findByAppointmentId(appointmentId));
    }

    @Transactional(readOnly = true)
    BillView find(long id) {
        return existing(id).toView(properties.zone());
    }

    @Transactional(readOnly = true)
    List<BillView> unpaid() {
        return views(bills.findByPaidOnIsNullOrderByIssuedOnAscNumberAsc());
    }

    BillView pay(long id, PaymentRequest payment) {
        var bill = locked(id);
        bill.pay(payment.method(), payment.paidOn(), today());
        return bill.toView(properties.zone());
    }

    BillView cancelPayment(long id) {
        var bill = locked(id);
        bill.cancelPayment();
        return bill.toView(properties.zone());
    }

    @Transactional(readOnly = true)
    ReceiptBook receipts(int year) {
        var paid = bills.findByPaidOnBetweenOrderByPaidOnAscNumberAsc(LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31));
        return ReceiptBook.of(year, paid.stream().map(Bill::toReceiptEntry).toList());
    }

    private Bill existing(long id) {
        return bills.findById(id)
            .orElseThrow(() -> new BillingRefusedException(HttpStatus.NOT_FOUND, "Note introuvable."));
    }

    private Bill locked(long id) {
        return bills.findForUpdateById(id)
            .orElseThrow(() -> new BillingRefusedException(HttpStatus.NOT_FOUND, "Note introuvable."));
    }

    private List<BillView> views(List<Bill> list) {
        return list.stream().map(bill -> bill.toView(properties.zone())).toList();
    }

    /** The date in Paris: at 00:30 in Paris, it is still yesterday in UTC. */
    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), properties.zone());
    }
}
