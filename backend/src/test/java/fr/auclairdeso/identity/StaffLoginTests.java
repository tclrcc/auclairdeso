package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import fr.auclairdeso.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
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
        createStaff("so@example.com", Role.PRACTITIONER);
        var session = loginWithMagicLink("so@example.com");

        assertThat(mvc.get().uri("/api/admin/staff").cookie(session)).hasStatus(HttpStatus.FORBIDDEN);

        var fullSession = loginWithPassword("so@example.com", session);

        assertThat(mvc.get().uri("/api/me").cookie(fullSession))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                        {"roles": ["PRACTITIONER"], "factors": ["OTT", "PASSWORD"], "passwordSet": true}
                        """);
        assertThat(mvc.get().uri("/api/admin/staff").cookie(fullSession)).hasStatusOk();
    }

    @Test
    void aWrongPasswordIsRejected() {
        createStaff("so-wrong@example.com", Role.PRACTITIONER);
        var session = loginWithMagicLink("so-wrong@example.com");

        assertThat(mvc.post().uri("/api/auth/password").with(csrf()).cookie(session)
            .param("username", "so-wrong@example.com")
            .param("password", "pas le bon mot de passe"))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aClientCannotReachTheAdminApi() {
        var session = loginWithMagicLink("curieux@example.com");

        assertThat(mvc.get().uri("/api/admin/staff").cookie(session)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyAnAdminCanResetAPasswordAndItEndsTheOwnersSessions() {
        createStaff("so-reset@example.com", Role.PRACTITIONER);
        createStaff("tony-reset@example.com", Role.ADMIN);
        var practitioner = loginWithPassword("so-reset@example.com", loginWithMagicLink("so-reset@example.com"));
        var admin = loginWithPassword("tony-reset@example.com", loginWithMagicLink("tony-reset@example.com"));

        assertThat(mvc.delete().uri("/api/admin/staff/so-reset@example.com/password").with(csrf()).cookie(practitioner))
            .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri("/api/admin/staff/so-reset@example.com/password").with(csrf()).cookie(admin))
            .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(accounts.findByEmail("so-reset@example.com")).get()
            .extracting(UserAccount::hasPassword).isEqualTo(false);
        assertThat(mvc.get().uri("/api/me").cookie(practitioner)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void createStaff(String email, Role role) {
        var account = UserAccount.withRole(email, role);
        account.changePassword(passwordEncoder.encode(PASSWORD));
        accounts.save(account);
    }

    private Cookie loginWithMagicLink(String email) {
        assertThat(mvc.post().uri("/api/auth/magic-link").with(csrf()).param("username", email))
            .hasStatus(HttpStatus.NO_CONTENT);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        var matcher = TOKEN.matcher(Objects.requireNonNull(captor.getValue().getText()));
        assertThat(matcher.find()).isTrue();

        var login = mvc.post().uri("/api/auth/magic-link/verify").with(csrf())
            .param("token", matcher.group(1))
            .exchange();
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);
        return sessionCookie(login, null);
    }

    private Cookie loginWithPassword(String email, Cookie session) {
        var login = mvc.post().uri("/api/auth/password").with(csrf()).cookie(session)
            .param("username", email)
            .param("password", PASSWORD)
            .exchange();
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);
        return sessionCookie(login, session);
    }

    /** The session cookie set by the response, or the previous one if the session id did not change. */
    private static Cookie sessionCookie(MvcTestResult result, Cookie previous) {
        var cookie = result.getResponse().getCookie("SESSION");
        assertThat(cookie != null || previous != null).isTrue();
        return cookie != null ? cookie : previous;
    }
}
