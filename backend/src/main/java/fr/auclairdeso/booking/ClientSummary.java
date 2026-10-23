package fr.auclairdeso.booking;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * What the practitioner sees about a client in her agenda.
 *
 * @param erased whether the client erased her account: what is left of her identity means nothing (9)
 */
record ClientSummary(Long id, @Nullable String email, String firstName, String lastName,
                     @Nullable LocalDate birthDate, String phone, @Nullable String city,
                     boolean trusted, boolean blocked, boolean erased) {
}
