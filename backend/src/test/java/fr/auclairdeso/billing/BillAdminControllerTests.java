package fr.auclairdeso.billing;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import fr.auclairdeso.WebSecurityTestConfiguration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest({BillAdminController.class, ReceiptBookController.class})
@Import(WebSecurityTestConfiguration.class)
class BillAdminControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private Bills bills;

    @Test
    void aClientNeverSeesTheReceiptBook() {
        assertThat(mvc.get().uri("/api/admin/receipts?year=2026").with(user("client@example.com").roles("CLIENT")))
            .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(bills);
    }

    @Test
    void aPaymentNeedsItsMethod() {
        assertThat(mvc.post().uri("/api/admin/bills/7/payment").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"method": null, "paidOn": "2026-10-19"}
                """))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.errors[*].field").asArray().contains("method");
        verifyNoInteractions(bills);
    }

    @Test
    void theReceiptBookDownloadsAsAFile() {
        given(bills.receipts(2026)).willReturn(ReceiptBook.of(2026, List.of(new ReceiptEntry(1L, 10L,
            LocalDate.of(2026, 10, 2), "2026-0001", "Alice Martin", "Consultation", PaymentMethod.TRANSFER, 8_000))));

        var result = mvc.get().uri("/api/admin/receipts/export?year=2026").with(practitioner()).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).startsWith("text/csv");
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
            .contains("attachment").contains("livre-des-recettes-2026.csv");
        assertThat(result).bodyText().contains("2026-0001;Alice Martin;Consultation;Virement;80,00");
    }
}
