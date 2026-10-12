package fr.auclairdeso.billing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The receipt book (livre des recettes), to read in the admin area or to download. */
@RestController
@RequestMapping("/admin/receipts")
class ReceiptBookController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final Bills bills;

    ReceiptBookController(Bills bills) {
        this.bills = bills;
    }

    @GetMapping
    ReceiptBook receipts(@RequestParam @Min(2000) @Max(2100) int year) {
        return bills.receipts(year);
    }

    @GetMapping("/export")
    ResponseEntity<String> export(@RequestParam @Min(2000) @Max(2100) int year) {
        var file = ContentDisposition.attachment().filename("livre-des-recettes-%d.csv".formatted(year)).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.toString())
            .contentType(CSV)
            .body(ReceiptBookCsv.of(bills.receipts(year)));
    }
}
