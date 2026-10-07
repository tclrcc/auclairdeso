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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits magic link requests per email address and per client, before any token is generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
class MagicLinkRateLimitFilter extends OncePerRequestFilter {

    static final String PATH = "/api/auth/magic-link";

    private static final String TOO_MANY_REQUESTS = """
            {"type": "about:blank", "title": "Too Many Requests", "status": 429, \
            "detail": "Trop de demandes de lien de connexion. Merci de réessayer dans quelques minutes."}""";

    private final AttemptLimiter perEmail = new AttemptLimiter(3, Duration.ofMinutes(15));
    private final AttemptLimiter perClient = new AttemptLimiter(20, Duration.ofHours(1));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var email = EmailAddresses.normalize(request.getParameter("username"));
        var allowed = perClient.tryAcquire(request.getRemoteAddr())
            && (email == null || perEmail.tryAcquire(email));
        if (allowed) {
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
