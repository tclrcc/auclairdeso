package fr.auclairdeso.billing;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The receipt book of a year (livre des recettes): the payments in date order, and their totals.
 *
 * @param totalsByMethod every payment method, even those with nothing received
 */
record ReceiptBook(int year, List<ReceiptEntry> entries, int totalCents, Map<PaymentMethod, Integer> totalsByMethod) {

    static ReceiptBook of(int year, List<ReceiptEntry> entries) {
        var byMethod = new EnumMap<PaymentMethod, Integer>(PaymentMethod.class);
        for (var method : PaymentMethod.values()) {
            byMethod.put(method, 0);
        }
        var total = 0;
        for (var entry : entries) {
            byMethod.merge(entry.method(), entry.amountCents(), Integer::sum);
            total += entry.amountCents();
        }
        return new ReceiptBook(year, List.copyOf(entries), total, byMethod);
    }
}
