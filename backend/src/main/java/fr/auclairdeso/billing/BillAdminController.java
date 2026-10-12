package fr.auclairdeso.billing;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bills")
class BillAdminController {

    private final Bills bills;

    BillAdminController(Bills bills) {
        this.bills = bills;
    }

    /** The bill of a session: a list, empty when the session has none. */
    @GetMapping
    List<BillView> ofAppointment(@RequestParam long appointmentId) {
        return bills.ofAppointment(appointmentId);
    }

    @GetMapping("/unpaid")
    List<BillView> unpaid() {
        return bills.unpaid();
    }

    @GetMapping("/{id}")
    BillView find(@PathVariable long id) {
        return bills.find(id);
    }

    @PostMapping("/{id}/payment")
    BillView pay(@PathVariable long id, @Valid @RequestBody PaymentRequest payment) {
        return bills.pay(id, payment);
    }

    @DeleteMapping("/{id}/payment")
    BillView cancelPayment(@PathVariable long id) {
        return bills.cancelPayment(id);
    }
}
