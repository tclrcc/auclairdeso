package fr.auclairdeso.booking;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

record ClosureRequest(
    @NotNull(message = "Le premier jour est obligatoire.") LocalDate firstDay,
    @NotNull(message = "Le dernier jour est obligatoire.") LocalDate lastDay,
    @Size(max = 200, message = "Le motif ne doit pas dépasser 200 caractères.") @Nullable String reason) {

    @AssertTrue(message = "Le dernier jour ne peut pas précéder le premier.")
    boolean isOrdered() {
        return firstDay == null || lastDay == null || !lastDay.isBefore(firstDay);
    }
}
