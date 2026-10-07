package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import fr.auclairdeso.WebSecurityTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(OfferingAdminController.class)
@Import(WebSecurityTestConfiguration.class)
class OfferingAdminControllerTests {

    private static final String VALID = """
            {"name": "Guidance — 1 h", "description": "Une séance complète.", "durationMinutes": 60,
             "bufferMinutes": 15, "priceCents": 8000, "paymentPolicy": "DEPOSIT_ONLINE", "depositCents": 3000,
             "displayOrder": 10, "active": true, "modes": ["PHONE", "VIDEO"]}
            """;

    private static final String INVALID = """
            {"name": "", "description": "Une séance.", "durationMinutes": 60,
             "bufferMinutes": 15, "priceCents": 8000, "paymentPolicy": "DEPOSIT_ONLINE", "depositCents": null,
             "displayOrder": 0, "active": true, "modes": []}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OfferingAdministration administration;

    @Test
    void createsAnOfferingAndPointsToIt() {
        given(administration.create(any())).willReturn(new OfferingDetails("guidance-1-h", "Guidance — 1 h",
            "Une séance complète.", 60, 15, 8_000, PaymentPolicy.DEPOSIT_ONLINE, 3_000, 10, true,
            List.of(ConsultationMode.VIDEO, ConsultationMode.PHONE)));

        var result = mvc.post().uri("/api/admin/offerings").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(VALID)
            .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/admin/offerings/guidance-1-h");
    }

    @Test
    void reportsEachInvalidField() {
        assertThat(mvc.post().uri("/api/admin/offerings").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(INVALID))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.errors[*].field")
            .asArray()
            .contains("name", "modes", "depositConsistent");
    }

    @Test
    void aClientIsForbidden() {
        assertThat(mvc.get().uri("/api/admin/offerings").with(user("client@example.com").roles("CLIENT")))
            .hasStatus(HttpStatus.FORBIDDEN);
    }

    /** A practitioner who logged in with the magic link and the password. */
    private static RequestPostProcessor practitioner() {
        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_PRACTITIONER"),
            FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.OTT_AUTHORITY),
            FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));
        return authentication(UsernamePasswordAuthenticationToken.authenticated("so@example.com", null, authorities));
    }
}
