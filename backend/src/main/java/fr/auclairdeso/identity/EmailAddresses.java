package fr.auclairdeso.identity;

import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

final class EmailAddresses {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailAddresses() {
    }

    /**
     * Trims and lower-cases an email address, or returns null if it is not plausibly valid.
     */
    static @Nullable String normalize(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        var email = raw.strip().toLowerCase(Locale.ROOT);
        return email.length() <= MAX_LENGTH && FORMAT.matcher(email).matches() ? email : null;
    }
}
