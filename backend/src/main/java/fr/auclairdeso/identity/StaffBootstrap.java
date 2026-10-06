package fr.auclairdeso.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the staff roles declared in the configuration, at every startup.
 */
@Component
class StaffBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffBootstrap.class);

    private final UserAccountRepository accounts;
    private final IdentityProperties properties;

    StaffBootstrap(UserAccountRepository accounts, IdentityProperties properties) {
        this.accounts = accounts;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        properties.practitioners().forEach(email -> apply(email, Role.PRACTITIONER));
        properties.admins().forEach(email -> apply(email, Role.ADMIN));
    }

    private void apply(String rawEmail, Role role) {
        var email = EmailAddresses.normalize(rawEmail);
        if (email == null) {
            throw new IllegalStateException("Invalid staff email in configuration: " + rawEmail);
        }
        var existing = accounts.findByEmail(email);
        if (existing.isEmpty()) {
            accounts.save(UserAccount.withRole(email, role));
            log.info("Created {} account for {}", role, email);
        } else if (existing.get().role() != role) {
            existing.get().grant(role);
            log.info("{} is now {}", email, role);
        }
    }
}
