package fr.auclairdeso;

import fr.auclairdeso.shared.security.SecurityConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;

/**
 * Our security rules for web slice tests, without the mail and database beans
 * the real magic link sender needs.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(SecurityConfiguration.class)
public class WebSecurityTestConfiguration {

    @Bean
    OneTimeTokenGenerationSuccessHandler magicLinkSender() {
        return (request, response, token) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
