package fr.auclairdeso.billing;

import fr.auclairdeso.booking.ClientErased;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A client erased her account: her bills stay, because the law keeps them ten years, but her email
 * address leaves them (docs/specification.md, 9).
 * <p>
 * Her name stays: the arrêté 83-50/A requires it on the note, and the receipt book is built from it.
 */
@Component
class ErasedClientBills {

    private final BillRepository bills;

    ErasedClientBills(BillRepository bills) {
        this.bills = bills;
    }

    @ApplicationModuleListener
    void on(ClientErased erased) {
        bills.clearEmail(erased.email());
    }
}
