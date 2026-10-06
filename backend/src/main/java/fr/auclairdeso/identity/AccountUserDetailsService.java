package fr.auclairdeso.identity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the user behind a consumed magic link.
 * An unknown email is a future client: owning the mailbox is the proof of identity,
 * and the account itself is created by {@link LoginRecorder} once the login succeeds.
 */
@Service
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accounts;

    AccountUserDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var role = accounts.findByEmail(email).map(UserAccount::role).orElse(Role.CLIENT);
        return User.withUsername(email)
            .password("")
            .roles(role.name())
            .build();
    }
}
