package fr.auclairdeso.booking;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** Details already known about a client, to prefill her next booking. */
record ClientProfile(String firstName, String lastName, @Nullable LocalDate birthDate, String phone,
                     @Nullable String city, boolean trusted) {
}
