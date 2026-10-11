package fr.auclairdeso.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A private note the practitioner adds to a client file. */
record NoteRequest(
    @NotBlank(message = "La note est vide.")
    @Size(max = 4000, message = "La note ne doit pas dépasser 4000 caractères.")
    String text) {
}
