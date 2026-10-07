package fr.auclairdeso.identity;

import java.nio.charset.StandardCharsets;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff passwords: defined or changed by their owner, reset by an administrator.
 */
@Service
class StaffPasswords {

    static final int MIN_LENGTH = 12;

    /** BCrypt only uses the first 72 bytes of a password. */
    static final int MAX_BYTES = 72;

    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    StaffPasswords(UserAccountRepository accounts, PasswordEncoder passwordEncoder,
                   FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    @Transactional
    void change(Authentication authentication, String newPassword) {
        var account = accounts.findByEmail(authentication.getName())
            .filter(found -> found.role().isStaff())
            .orElseThrow(() -> new AccessDeniedException("Only staff members have a password"));

        // Defining a first password needs the magic link; changing it also needs the current one.
        var missingFactor = !hasFactor(authentication, FactorGrantedAuthority.OTT_AUTHORITY)
            || (account.hasPassword() && !hasFactor(authentication, FactorGrantedAuthority.PASSWORD_AUTHORITY));
        if (missingFactor) {
            throw new AccessDeniedException("Missing authentication factor");
        }

        checkStrength(newPassword);
        account.changePassword(passwordEncoder.encode(newPassword));
    }

    /**
     * Clears a staff member's password and ends all their sessions.
     * They will choose a new password at their next login.
     *
     * @return false if there is no staff member with this email
     */
    @Transactional
    boolean reset(String rawEmail) {
        var email = EmailAddresses.normalize(rawEmail);
        if (email == null) {
            return false;
        }
        var account = accounts.findByEmail(email).filter(found -> found.role().isStaff());
        if (account.isEmpty()) {
            return false;
        }
        account.get().clearPassword();
        sessions.findByPrincipalName(email).keySet().forEach(sessions::deleteById);
        return true;
    }

    private static void checkStrength(String password) {
        if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins %d caractères.".formatted(MIN_LENGTH));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidPasswordException("Le mot de passe est trop long.");
        }
    }

    private static boolean hasFactor(Authentication authentication, String factor) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> factor.equals(authority.getAuthority()));
    }
}
