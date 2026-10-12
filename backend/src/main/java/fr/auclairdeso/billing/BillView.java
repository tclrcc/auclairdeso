package fr.auclairdeso.billing;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** A bill, as the admin area shows and prints it. */
record BillView(Long id, String number, Long appointmentId, LocalDate issuedOn, String practitionerName,
                String practitionerAddress, String siret, String clientName, @Nullable String clientEmail,
                OffsetDateTime serviceStart, String servicePlace, String description, int amountCents,
                String vatMention, @Nullable PaymentMethod paymentMethod, @Nullable LocalDate paidOn) {
}
