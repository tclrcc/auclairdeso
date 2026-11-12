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
 * Limits magic link requests per client address, before any token is generated.
 *
 * <p>The answer depends on the caller only, never on the email address asked for: the limits per
 * address are in {@link MagicLinkQuota}, which answers the same whether the link leaves or not.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
class MagicLinkRateLimitFilter extends OncePerRequestFilter {

    static final String PATH = "/api/auth/magic-link";

    private static final String TOO_MANY_REQUESTS = """
            {"type": "about:blank", "title": "Too Many Requests", "status": 429, \
            "detail": "Trop de demandes de lien de connexion. Merci de réessayer dans quelques minutes."}""";

    /** Matches the path as Spring Security does, so that an encoded variant of it is caught as well. */
    private final RequestMatcher magicLink = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, PATH);
    private final AttemptLimiter perClient = new AttemptLimiter(20, Duration.ofHours(1));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !magicLink.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (perClient.tryAcquire(request.getRemoteAddr())) {
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
