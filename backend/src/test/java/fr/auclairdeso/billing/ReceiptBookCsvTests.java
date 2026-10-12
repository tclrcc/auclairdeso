package fr.auclairdeso.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptBookCsvTests {

    @Test
    void excelOpensItInFrenchWithATotal() {
        var book = ReceiptBook.of(2026, List.of(
            new ReceiptEntry(1L, 10L, LocalDate.of(2026, 10, 2), "2026-0001", "Alice Martin",
                "Consultation approfondie", PaymentMethod.TRANSFER, 8_000),
            new ReceiptEntry(2L, 11L, LocalDate.of(2026, 10, 9), "2026-0002", "Chloé Bernard",
                "Coupage de feu", PaymentMethod.CASH, 4_050)));

        var lines = ReceiptBookCsv.of(book).split("\r\n");

        // The byte order mark first: without it, Excel reads the accents as Latin-1.
        assertThat(lines[0])
            .isEqualTo("\uFEFFDate d'encaissement;N° de note;Cliente;Prestation;Mode de règlement;Montant (€)");
        assertThat(lines[1]).isEqualTo("02/10/2026;2026-0001;Alice Martin;Consultation approfondie;Virement;80,00");
        assertThat(lines[2]).isEqualTo("09/10/2026;2026-0002;Chloé Bernard;Coupage de feu;Espèces;40,50");
        assertThat(lines[3]).isEqualTo("Total;;;;;120,50");
        assertThat(book.totalsByMethod().get(PaymentMethod.MOBILE)).isZero();
    }

    @Test
    void aNameNeverBecomesAFormulaNorBreaksTheColumns() {
        assertThat(ReceiptBookCsv.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(ReceiptBookCsv.cell("Martin; Alice")).isEqualTo("\"Martin; Alice\"");
        assertThat(ReceiptBookCsv.cell("Alice Martin")).isEqualTo("Alice Martin");
    }
}
