package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import fr.auclairdeso.TestcontainersConfiguration;

import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class StaffLoginTests {

    private static final Pattern TOKEN = Pattern.compile("token=([0-9a-f-]{36})");
    private static final String PASSWORD = "une phrase de passe solide";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void theAdminApiNeedsTheMagicLinkAndThePassword() {
        createPractitioner("so@example.com");
        var session = loginWithMagicLink("so@example.com");

        assertThat(mvc.get().uri("/api/admin/staff").session(session)).hasStatus(HttpStatus.FORBIDDEN);

        var passwordLogin = mvc.post().uri("/api/auth/password").with(csrf()).session(session)
            .param("username", "so@example.com")
            .param("password", PASSWORD)
            .exchange();
        assertThat(passwordLogin).hasStatus(HttpStatus.NO_CONTENT);
        var fullSession = (MockHttpSession) passwordLogin.getRequest().getSession(false);

        assertThat(mvc.get().uri("/api/me").session(fullSession))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                        {"roles": ["PRACTITIONER"], "factors": ["OTT", "PASSWORD"], "passwordSet": true}
                        """);
        assertThat(mvc.get().uri("/api/admin/staff").session(fullSession)).hasStatusOk();
    }

    @Test
    void aWrongPasswordIsRejected() {
        createPractitioner("so-wrong@example.com");
        var session = loginWithMagicLink("so-wrong@example.com");

        assertThat(mvc.post().uri("/api/auth/password").with(csrf()).session(session)
            .param("username", "so-wrong@example.com")
            .param("password", "pas le bon mot de passe"))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aClientCannotReachTheAdminApi() {
        var session = loginWithMagicLink("curieux@example.com");

        assertThat(mvc.get().uri("/api/admin/staff").session(session)).hasStatus(HttpStatus.FORBIDDEN);
    }

    private void createPractitioner(String email) {
        var account = UserAccount.withRole(email, Role.PRACTITIONER);
        account.changePassword(passwordEncoder.encode(PASSWORD));
        accounts.save(account);
    }

    private MockHttpSession loginWithMagicLink(String email) {
        assertThat(mvc.post().uri("/api/auth/magic-link").with(csrf()).param("username", email))
            .hasStatus(HttpStatus.NO_CONTENT);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        var matcher = TOKEN.matcher(Objects.requireNonNull(captor.getValue().getText()));
        assertThat(matcher.find()).isTrue();

        var login = mvc.post().uri("/api/auth/magic-link/verify").with(csrf())
            .param("token", matcher.group(1))
            .exchange();
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
