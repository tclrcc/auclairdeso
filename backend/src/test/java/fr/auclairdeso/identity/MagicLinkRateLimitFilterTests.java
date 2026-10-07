package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MagicLinkRateLimitFilterTests {

    private final MagicLinkRateLimitFilter filter = new MagicLinkRateLimitFilter();

    @Test
    void blocksTheFourthRequestForTheSameEmail() throws Exception {
        for (var i = 1; i <= 3; i++) {
            assertThat(requestLink("Client@Example.com", "10.0.0." + i).getStatus()).isEqualTo(200);
        }

        var blocked = requestLink("client@example.com", "10.0.0.9");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
        assertThat(blocked.getContentAsString()).contains("Trop de demandes");
    }

    @Test
    void leavesOtherRequestsAlone() throws Exception {
        for (var i = 0; i < 10; i++) {
            var request = new MockHttpServletRequest("POST", "/api/auth/magic-link/verify");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse requestLink(String email, String clientIp) throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", MagicLinkRateLimitFilter.PATH);
        request.setParameter("username", email);
        request.setRemoteAddr(clientIp);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
