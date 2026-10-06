package fr.auclairdeso.identity;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenRequestResolver;

@Configuration(proxyBeanMethods = false)
class MagicLinkConfiguration {

    static final Duration TOKEN_VALIDITY = Duration.ofMinutes(10);

    @Bean
    OneTimeTokenService oneTimeTokenService(JdbcOperations jdbcOperations) {
        return new JdbcOneTimeTokenService(jdbcOperations);
    }

    @Bean
    GenerateOneTimeTokenRequestResolver generateOneTimeTokenRequestResolver() {
        return request -> {
            var email = EmailAddresses.normalize(request.getParameter("username"));
            return email == null ? null : new GenerateOneTimeTokenRequest(email, TOKEN_VALIDITY);
        };
    }
}
