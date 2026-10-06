package fr.auclairdeso.identity;

import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the account on first login and records each successful login.
 */
@Component
class LoginRecorder {

    private final UserAccountRepository accounts;

    LoginRecorder(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @EventListener
    @Transactional
    void onLogin(AuthenticationSuccessEvent event) {
        var email = event.getAuthentication().getName();
        var account = accounts.findByEmail(email)
            .orElseGet(() -> accounts.save(UserAccount.newClient(email)));
        account.recordLogin(Instant.now());
    }
}
