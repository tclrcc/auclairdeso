package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import fr.auclairdeso.TestcontainersConfiguration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class MagicLinkLoginTests {

    private static final Pattern TOKEN = Pattern.compile("token=([0-9a-f-]{36})");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserAccountRepository accounts;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void magicLinkLogsTheUserInAndCreatesTheirAccount() {
        assertThat(mvc.post().uri("/api/auth/magic-link").with(csrf())
            .param("username", "  Client@Example.com "))
            .hasStatus(HttpStatus.NO_CONTENT);

        var token = tokenSentTo("client@example.com");

        var login = mvc.post().uri("/api/auth/magic-link/verify").with(csrf())
            .param("token", token)
            .exchange();
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);
        var session = (MockHttpSession) login.getRequest().getSession(false);

        assertThat(mvc.get().uri("/api/me").session(session))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                        {"email": "client@example.com", "roles": ["CLIENT"]}
                        """);
        assertThat(accounts.findByEmail("client@example.com")).isPresent();
    }

    @Test
    void aTokenWorksOnlyOnce() {
        mvc.post().uri("/api/auth/magic-link").with(csrf())
            .param("username", "once@example.com")
            .exchange();
        var token = tokenSentTo("once@example.com");

        assertThat(mvc.post().uri("/api/auth/magic-link/verify").with(csrf()).param("token", token))
            .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.post().uri("/api/auth/magic-link/verify").with(csrf()).param("token", token))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private String tokenSentTo(String expectedRecipient) {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        var message = captor.getValue();
        assertThat(message.getTo()).containsExactly(expectedRecipient);
        var matcher = TOKEN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
