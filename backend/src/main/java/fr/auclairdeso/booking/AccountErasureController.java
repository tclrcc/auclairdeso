package fr.auclairdeso.booking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountErasureController {

    private final ClientErasure erasure;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    AccountErasureController(ClientErasure erasure) {
        this.erasure = erasure;
    }

    /**
     * A client erases her own account and her data. Only her own: the email comes from her session.
     * <p>
     * Her session is closed here, before the answer leaves, rather than waiting for the listener that
     * deletes her login: she must not stay logged in with an account that no longer exists.
     */
    @DeleteMapping("/account")
    ErasureReport erase(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        var report = erasure.erase(authentication.getName());
        logout.logout(request, response, authentication);
        return report;
    }
}
