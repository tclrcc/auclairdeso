package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import fr.auclairdeso.WebSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OfferingController.class)
@Import(WebSecurityTestConfiguration.class)
class OfferingControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OfferingCatalog catalog;

    @Test
    void listsActiveOfferingsUnderTheApiPrefix() {
        given(catalog.findActiveOfferings()).willReturn(List.of(guidance()));

        assertThat(mvc.get().uri("/api/offerings"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$[0].slug").isEqualTo("guidance-1-h");
    }

    @Test
    void unknownSlugReturnsAProblemDetail() {
        given(catalog.findActiveOffering("inconnue")).willReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/offerings/inconnue"))
            .hasStatus(HttpStatus.NOT_FOUND)
            .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private static OfferingView guidance() {
        return new OfferingView("guidance-1-h", "Guidance — 1 h", "Description", 60, 8_000,
            PaymentPolicy.FULL_ONLINE, null, List.of(ConsultationMode.PHONE));
    }
}
