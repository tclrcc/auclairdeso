package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** A booking request sent by a client (docs/specification.md, 5.1). */
record BookingRequest(
    @NotBlank(message = "La séance est obligatoire.")
    String offeringSlug,

    @NotNull(message = "Le mode de consultation est obligatoire.")
    ConsultationMode mode,

    @NotNull(message = "Le créneau est obligatoire.")
    OffsetDateTime start,

    @NotNull(message = "Vos coordonnées sont obligatoires.")
    @Valid ClientDetails client,

    @NotBlank(message = "Indiquez la raison de votre demande.")
    @Size(max = 2000, message = "La raison ne doit pas dépasser 2000 caractères.")
    String reason,

    @Size(max = 300, message = "L'adresse ne doit pas dépasser 300 caractères.")
    @Nullable String address,

    @Size(max = 100, message = "Le nom Messenger ne doit pas dépasser 100 caractères.")
    @Nullable String messengerName,

    @AssertTrue(message = "Vous devez être majeur(e) et accepter les conditions générales.")
    boolean acceptedTerms,

    @AssertTrue(message = "Votre accord est nécessaire pour le traitement de vos données.")
    boolean consentsToDataProcessing) {
}
