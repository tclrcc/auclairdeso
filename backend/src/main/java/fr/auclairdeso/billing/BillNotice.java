package fr.auclairdeso.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Everything printed on a bill, for the other modules (the email to the client today).
 *
 * @param serviceStart in Paris time
 */
public record BillNotice(String number, LocalDate issuedOn, String practitionerName, String practitionerAddress,
                         String siret, String clientFirstName, String clientName, @Nullable String clientEmail,
                         LocalDateTime serviceStart, String servicePlace, String description, int amountCents,
                         String vatMention) {
}
