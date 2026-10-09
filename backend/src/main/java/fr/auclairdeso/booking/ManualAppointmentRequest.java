package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * An appointment entered by the practitioner, for an existing client (clientId) or a new one (newClient).
 * The start is a local time in the practitioner's time zone.
 */
record ManualAppointmentRequest(
    @NotBlank(message = "La séance est obligatoire.")
    String offeringSlug,

    @NotNull(message = "Le mode de consultation est obligatoire.")
    ConsultationMode mode,

    @NotNull(message = "La date et l'heure sont obligatoires.")
    LocalDateTime start,

    @Nullable Long clientId,

    @Valid @Nullable ManualClient newClient,

    @Size(max = 2000, message = "La note ne doit pas dépasser 2000 caractères.")
    @Nullable String note,

    @Size(max = 300, message = "L'adresse ne doit pas dépasser 300 caractères.")
    @Nullable String address,

    @Size(max = 100, message = "Le nom Messenger ne doit pas dépasser 100 caractères.")
    @Nullable String messengerName) {

    @AssertTrue(message = "Choisissez une cliente existante, ou décrivez une nouvelle cliente.")
    boolean isClientChosen() {
        return (clientId == null) != (newClient == null);
    }
}
