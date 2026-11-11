package fr.auclairdeso.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Everything the practitioner can set on an offering, except its slug. Amounts are in euro cents. The payment
 * policy and the deposit are always forced to ON_SITE and nothing, whatever is received.
 */
record OfferingDraft(
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères.")
    @Pattern(regexp = ".*[\\p{L}\\p{N}].*", message = "Le nom doit contenir au moins une lettre ou un chiffre.")
    String name,

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 4000, message = "La description ne doit pas dépasser 4000 caractères.")
    String description,

    @Min(value = 15, message = "La durée minimale est de 15 minutes.")
    @Max(value = 240, message = "La durée maximale est de 240 minutes.")
    int durationMinutes,

    @Min(value = 0, message = "La pause ne peut pas être négative.")
    @Max(value = 120, message = "La pause ne doit pas dépasser 120 minutes.")
    int bufferMinutes,

    @Min(value = 0, message = "Le prix ne peut pas être négatif.")
    @Max(value = 1_000_000, message = "Le prix ne doit pas dépasser 10 000 €.")
    int priceCents,

    PaymentPolicy paymentPolicy,

    @Nullable Integer depositCents,

    @Min(value = 0, message = "L'ordre d'affichage ne peut pas être négatif.")
    int displayOrder,

    boolean active,

    @NotEmpty(message = "Choisissez au moins un mode de consultation.")
    Set<ConsultationMode> modes,

    @NotNull(message = "La catégorie est obligatoire.")
    OfferingCategory category) {

    /**
     * The site takes no online payment (docs/specification.md, 3.5): whatever the caller sends, an offering is
     * paid on site and carries no deposit. The enum and the column stay as they are.
     */
    OfferingDraft {
        paymentPolicy = PaymentPolicy.ON_SITE;
        depositCents = null;
    }
}
