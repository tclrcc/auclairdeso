package fr.auclairdeso.billing;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** The practitioner records how and when a bill was paid. */
record PaymentRequest(
    @NotNull(message = "Choisissez le moyen de paiement.")
    PaymentMethod method,

    @NotNull(message = "La date d'encaissement est obligatoire.")
    LocalDate paidOn) {
}
