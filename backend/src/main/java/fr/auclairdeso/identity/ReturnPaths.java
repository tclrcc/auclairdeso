package fr.auclairdeso.identity;

import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Where to send the user back after login: only a path of this site, never another domain
 * ("//evil.example" or "/\evil.example" would be read by browsers as another site).
 */
final class ReturnPaths {

    private static final Pattern SAFE = Pattern.compile("^/(?![/\\\\])[A-Za-z0-9/_.~%?=&:+-]{0,300}$");

    private ReturnPaths() {
    }

    static Optional<String> from(@Nullable String value) {
        return value != null && SAFE.matcher(value).matches() ? Optional.of(value) : Optional.empty();
    }
}
