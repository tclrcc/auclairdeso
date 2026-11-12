package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTests {

    private final LoginRateLimitFilter filter = new LoginRateLimitFilter();

    @Test
    void blocksAClientAfterTwentyLinkRequestsInAnHour() throws Exception {
        for (var i = 0; i < 20; i++) {
            assertThat(post(LoginRateLimitFilter.MAGIC_LINK_PATH, "client" + i + "@example.com", "10.0.0.1").getStatus())
                .isEqualTo(200);
        }

        var blocked = post(LoginRateLimitFilter.MAGIC_LINK_PATH, "other@example.com", "10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
        assertThat(blocked.getContentAsString()).contains("Trop de demandes");
        assertThat(post(LoginRateLimitFilter.MAGIC_LINK_PATH, "other@example.com", "10.0.0.2").getStatus())
            .isEqualTo(200);
    }

    @Test
    void neverLimitsOnTheEmailAddress() throws Exception {
        // The limits per address answer as if the link had left (MagicLinkQuota): never a 429 here.
        for (var i = 1; i <= 10; i++) {
            assertThat(post(LoginRateLimitFilter.MAGIC_LINK_PATH, "client@example.com", "10.0.1." + i).getStatus())
                .isEqualTo(200);
        }
    }

    @Test
    void countsAnEncodedSpellingOfThePath() throws Exception {
        for (var i = 0; i < 20; i++) {
            post("/api/auth/magic%2Dlink", "client" + i + "@example.com", "10.0.2.1");
        }

        assertThat(post(LoginRateLimitFilter.MAGIC_LINK_PATH, "other@example.com", "10.0.2.1").getStatus())
            .isEqualTo(429);
    }

    @Test
    void blocksTheEleventhPasswordAttemptOfAClientInFifteenMinutes() throws Exception {
        for (var i = 0; i < 10; i++) {
            assertThat(post(LoginRateLimitFilter.PASSWORD_PATH, "so@example.com", "10.0.3.1").getStatus())
                .isEqualTo(200);
        }

        assertThat(post(LoginRateLimitFilter.PASSWORD_PATH, "so@example.com", "10.0.3.1").getStatus())
            .isEqualTo(429);
        // The account itself is not locked: from elsewhere, the staff member can still log in.
        assertThat(post(LoginRateLimitFilter.PASSWORD_PATH, "so@example.com", "10.0.3.2").getStatus())
            .isEqualTo(200);
        // And password attempts do not use up the magic link requests of that client.
        assertThat(post(LoginRateLimitFilter.MAGIC_LINK_PATH, "so@example.com", "10.0.3.1").getStatus())
            .isEqualTo(200);
    }

    @Test
    void leavesOtherRequestsAlone() throws Exception {
        for (var i = 0; i < 30; i++) {
            var request = new MockHttpServletRequest("POST", "/api/auth/magic-link/verify");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse post(String path, String email, String clientIp)
        throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", path);
        request.setParameter("username", email);
        request.setRemoteAddr(clientIp);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
