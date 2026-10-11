package fr.auclairdeso.identity;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Who the staff are, for the other modules: for instance, whom to tell about a new request.
 * The only public door of the identity module to its configuration.
 */
@Component
public class StaffDirectory {

    private final IdentityProperties properties;

    StaffDirectory(IdentityProperties properties) {
        this.properties = properties;
    }

    /** The practitioners' email addresses (AUCLAIRDESO_IDENTITY_PRACTITIONERS). */
    public List<String> practitionerEmails() {
        return properties.practitioners().stream()
            .map(email -> email.strip().toLowerCase(Locale.ROOT))
            .filter(email -> !email.isEmpty())
            .distinct()
            .toList();
    }
}
