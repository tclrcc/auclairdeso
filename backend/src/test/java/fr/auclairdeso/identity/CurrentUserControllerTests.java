package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import fr.auclairdeso.WebSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CurrentUserController.class)
@Import(WebSecurityTestConfiguration.class)
class CurrentUserControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private UserAccountRepository accounts;

    @Test
    void anonymousVisitorGetsUnauthorized() {
        assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(username = "client@example.com", roles = "CLIENT")
    void authenticatedUserSeesTheirEmailAndRoles() {
        assertThat(mvc.get().uri("/api/me"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                        {"email": "client@example.com", "roles": ["CLIENT"], "factors": [], "passwordSet": false}
                        """);
    }

    @Test
    void unsafeRequestWithoutCsrfTokenIsForbidden() {
        assertThat(mvc.post().uri("/api/me")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void unsafeRequestWithCsrfTokenReachesAuthentication() {
        assertThat(mvc.post().uri("/api/me").with(csrf())).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
