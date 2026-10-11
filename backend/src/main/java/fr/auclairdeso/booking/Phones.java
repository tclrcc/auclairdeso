package fr.auclairdeso.booking;

/**
 * French phone numbers, however they are written: "06 12 34 56 78", "06.12.34.56.78",
 * "+33 6 12 34 56 78" or "0033 6 12 34 56 78" are the same number.
 */
final class Phones {

    private static final String FRANCE = "33";

    private Phones() {
    }

    /** Only the digits, in the national form: "+33 6 12 34 56 78" becomes "0612345678". */
    static String normalize(String phone) {
        var digits = phone.replaceAll("\\D", "");
        String national;
        if (digits.startsWith("00" + FRANCE)) {
            national = digits.substring(4);
        } else if (phone.strip().startsWith("+" + FRANCE)) {
            national = digits.substring(2);
        } else {
            return digits;
        }
        // "+33 06 12 34 56 78" is a frequent way to write it: the 0 is already there.
        return national.startsWith("0") ? national : "0" + national;
    }

    static boolean same(String first, String second) {
        return normalize(first).equals(normalize(second));
    }
}
