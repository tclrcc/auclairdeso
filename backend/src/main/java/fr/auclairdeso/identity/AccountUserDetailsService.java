package fr.auclairdeso.identity;

import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads users for both login methods: magic link and password.
 * An unknown email is a future client: owning the mailbox is the proof of identity,
 * and the account itself is created by {@link LoginRecorder} once the login succeeds.
 */
@Service
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accounts;

    /** Hash of a random value nobody knows: accounts without a password can never use one. */
    private final String unusablePassword;

    AccountUserDetailsService(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var email = EmailAddresses.normalize(username);
        if (email == null) {
            throw new UsernameNotFoundException("Not a valid email address");
        }
        var account = accounts.findByEmail(email);
        var role = account.map(UserAccount::role).orElse(Role.CLIENT);
        var password = account.map(UserAccount::passwordHash).orElse(unusablePassword);
        return User.withUsername(email)
            .password(password)
            .roles(role.name())
            .build();
    }
}
