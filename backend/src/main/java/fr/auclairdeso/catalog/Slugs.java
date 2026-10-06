package fr.auclairdeso.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns an offering name into a URL slug: "Cœur à cœur — 1 h" becomes "coeur-a-coeur-1-h".
 */
final class Slugs {

    static final int MAX_LENGTH = 80;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private Slugs() {
    }

    static String from(String text) {
        var lower = text.toLowerCase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae");
        var ascii = DIACRITICS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        var slug = trimDashes(SEPARATORS.matcher(ascii).replaceAll("-"));
        if (slug.length() > MAX_LENGTH) {
            slug = trimDashes(slug.substring(0, MAX_LENGTH));
        }
        return slug.isEmpty() ? "seance" : slug;
    }

    private static String trimDashes(String value) {
        return EDGE_DASHES.matcher(value).replaceAll("");
    }
}
