package fr.auclairdeso.billing;

import java.time.LocalDate;

/** One line of the receipt book: a payment, with the bill it pays. */
record ReceiptEntry(Long billId, Long appointmentId, LocalDate paidOn, String number, String clientName,
                    String description, PaymentMethod method, int amountCents) {
}
