package fr.auclairdeso.billing;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The receipt book as a CSV file that Excel and LibreOffice open as they are, in French:
 * semicolons, decimal commas, UTF-8 with a byte order mark, Windows line endings.
 */
final class ReceiptBookCsv {

    private static final String LINE = "\r\n";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern NEEDS_QUOTES = Pattern.compile("[;\"\r\n]");

    private ReceiptBookCsv() {
    }

    static String of(ReceiptBook book) {
        var csv = new StringBuilder("\uFEFF");
        csv.append("Date d'encaissement;N° de note;Cliente;Prestation;Mode de règlement;Montant (€)").append(LINE);
        for (var entry : book.entries()) {
            csv.append(DATE.format(entry.paidOn())).append(';')
                .append(cell(entry.number())).append(';')
                .append(cell(entry.clientName())).append(';')
                .append(cell(entry.description())).append(';')
                .append(entry.method().label()).append(';')
                .append(amount(entry.amountCents())).append(LINE);
        }
        csv.append("Total;;;;;").append(amount(book.totalCents())).append(LINE);
        return csv.toString();
    }

    /** "1234,50": a decimal comma, and no thousands separator that would turn the number into text. */
    static String amount(int cents) {
        return String.format(Locale.ROOT, "%d,%02d", cents / 100, cents % 100);
    }

    /**
     * A text cell. Names come from clients: "=..." would become a formula when the file is opened,
     * so such a cell starts with an apostrophe (CSV injection).
     */
    static String cell(String value) {
        var text = value;
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return NEEDS_QUOTES.matcher(text).find() ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
