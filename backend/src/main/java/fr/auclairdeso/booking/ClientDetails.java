package fr.auclairdeso.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** What a client tells about herself when booking (docs/specification.md, 5.2). */
record ClientDetails(
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères.")
    String firstName,

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères.")
    String lastName,

    @NotNull(message = "La date de naissance est obligatoire.")
    @Past(message = "La date de naissance doit être dans le passé.")
    LocalDate birthDate,

    @NotBlank(message = "Le téléphone est obligatoire.")
    @Pattern(regexp = "^\\+?[0-9][0-9 .-]{8,18}$", message = "Ce numéro de téléphone ne semble pas valide.")
    String phone,

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Nullable String city) {

    boolean isAdultOn(LocalDate day) {
        return !birthDate.plusYears(18).isAfter(day);
    }
}
