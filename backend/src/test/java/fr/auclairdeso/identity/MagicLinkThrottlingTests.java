package fr.auclairdeso.identity;

import static fr.auclairdeso.TestcontainersConfiguration.NO_MAIL_HEALTH_CHECK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import fr.auclairdeso.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Same configuration as MagicLinkLoginTests, so that both share one application context. */
@SpringBootTest(properties = NO_MAIL_HEALTH_CHECK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class MagicLinkThrottlingTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void theFourthLinkInFifteenMinutesIsNotSentAndTheAnswerDoesNotShowIt() {
        var first = requestLink("limited@example.com", "10.1.0.1");
        requestLink("limited@example.com", "10.1.0.2");
        requestLink("limited@example.com", "10.1.0.3");

        var fourth = requestLink("limited@example.com", "10.1.0.4");

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        assertThat(fourth.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value()).isEqualTo(first.getStatus());
        assertThat(fourth.getContentAsByteArray()).isEmpty();
        assertThat(fourth.getHeaderNames()).containsExactlyInAnyOrderElementsOf(first.getHeaderNames());
    }

    @Test
    void anotherAddressIsNotAffected() {
        for (var i = 1; i <= 4; i++) {
            requestLink("busy@example.com", "10.2.0." + i);
        }

        var answer = requestLink("calm@example.com", "10.2.0.9");

        assertThat(answer.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(4)).send(captor.capture());
        assertThat(captor.getAllValues().getLast().getTo()).containsExactly("calm@example.com");
    }

    /** Each request comes from its own client address, so that only the limits per email are at play. */
    private MockHttpServletResponse requestLink(String email, String clientIp) {
        var result = mvc.post().uri("/api/auth/magic-link").with(csrf())
            .with(request -> {
                request.setRemoteAddr(clientIp);
                return request;
            })
            .param("username", email)
            .exchange();
        return result.getResponse();
    }
}
