package fr.auclairdeso.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.SingleResultAuthorizationManager;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration(proxyBeanMethods = false)
@EnableMultiFactorAuthentication(authorities = {})
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OneTimeTokenGenerationSuccessHandler magicLinkSender,
                                            @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled)
        throws Exception {
        var bothFactors = AuthorizationManagerFactories.multiFactor()
            .requireFactors(FactorGrantedAuthority.OTT_AUTHORITY, FactorGrantedAuthority.PASSWORD_AUTHORITY)
            .build();
        AuthenticationSuccessHandler noContent =
            (request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value());
        AuthenticationFailureHandler unauthorized =
            (request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value());

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Open only where springdoc is on (development): closed here too, not only by the gateway.
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .access(apiDocsEnabled ? SingleResultAuthorizationManager.permitAll()
                    : SingleResultAuthorizationManager.denyAll())
                .requestMatchers(HttpMethod.GET, "/api/offerings/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/legal").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/account").hasRole("CLIENT")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/staff/*/password")
                .access(bothFactors.hasRole("ADMIN"))
                .requestMatchers("/api/admin/**").access(bothFactors.hasAnyRole("PRACTITIONER", "ADMIN"))
                .requestMatchers("/api/account/password").hasAnyRole("PRACTITIONER", "ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            // HSTS is switched on in one place only, the rendering server (HSTS_MAX_AGE, deploy/.env.example),
            // once the definitive domain is served over HTTPS. The other default headers stay.
            .headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts.disable()))
            .csrf(CsrfConfigurer::spa)
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .oneTimeTokenLogin(ott -> ott
                .tokenGeneratingUrl("/api/auth/magic-link")
                .tokenGenerationSuccessHandler(magicLinkSender)
                .loginProcessingUrl("/api/auth/magic-link/verify")
                .loginPage("/connexion")
                .showDefaultSubmitPage(false)
                .successHandler(noContent)
                .failureHandler(unauthorized))
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/password")
                .loginPage("/connexion")
                .successHandler(noContent)
                .failureHandler(unauthorized))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new AccessDeniedHandlerImpl()));
        return http.build();
    }
}
