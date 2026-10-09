package fr.auclairdeso.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** A new client entered by the practitioner: only her name and phone number are required. */
record ManualClient(
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères.")
    String firstName,

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères.")
    String lastName,

    @NotBlank(message = "Le téléphone est obligatoire.")
    @Pattern(regexp = "^\\+?[0-9][0-9 .-]{8,18}$", message = "Ce numéro de téléphone ne semble pas valide.")
    String phone,

    @Email(message = "Cette adresse email ne semble pas valide.")
    @Size(max = 254, message = "L'adresse email est trop longue.")
    @Nullable String email,

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Nullable String city) {
}
