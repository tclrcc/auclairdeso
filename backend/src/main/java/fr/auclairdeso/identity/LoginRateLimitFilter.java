package fr.auclairdeso.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits login requests per client address, before Spring Security handles them: magic link
 * requests, and password attempts of the staff.
 *
 * <p>The answer depends on the caller only, never on the email address or the account: the limits
 * per address are in {@link MagicLinkQuota}, which answers the same whether the link leaves or not.
 * There is no account lockout: with two staff accounts, it would give anyone a way to lock them out.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String MAGIC_LINK_PATH = "/api/auth/magic-link";
    static final String PASSWORD_PATH = "/api/auth/password";

    private static final String TOO_MANY_REQUESTS = """
            {"type": "about:blank", "title": "Too Many Requests", "status": 429, \
            "detail": "Trop de demandes de connexion. Merci de réessayer dans quelques minutes."}""";

    // Matched as Spring Security matches them, so that an encoded variant of a path is caught as well.
    private final RequestMatcher magicLink = PathPatternRequestMatcher.withDefaults()
        .matcher(HttpMethod.POST, MAGIC_LINK_PATH);
    private final RequestMatcher password = PathPatternRequestMatcher.withDefaults()
        .matcher(HttpMethod.POST, PASSWORD_PATH);

    private final AttemptLimiter magicLinksPerClient = new AttemptLimiter(20, Duration.ofHours(1));
    private final AttemptLimiter passwordsPerClient = new AttemptLimiter(10, Duration.ofMinutes(15));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !magicLink.matches(request) && !password.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var limiter = magicLink.matches(request) ? magicLinksPerClient : passwordsPerClient;
        if (limiter.tryAcquire(request.getRemoteAddr())) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, "900");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        response.getWriter().write(TOO_MANY_REQUESTS);
    }
}
