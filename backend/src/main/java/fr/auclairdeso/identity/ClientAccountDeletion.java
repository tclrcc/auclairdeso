package fr.auclairdeso.identity;

import fr.auclairdeso.booking.ClientErased;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * A client erased her account: her login goes with it, along with the magic links still valid and the
 * sessions she left open elsewhere (docs/specification.md, 9).
 * <p>
 * It runs after the commit of the erasure: if that had failed, the login would stay. The event waits
 * in the publication table until this listener succeeds, so a login is never left behind.
 */
@Component
class ClientAccountDeletion {

    private final UserAccountRepository accounts;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final JdbcOperations jdbc;

    ClientAccountDeletion(UserAccountRepository accounts,
                          FindByIndexNameSessionRepository<? extends Session> sessions, JdbcOperations jdbc) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.jdbc = jdbc;
    }

    @ApplicationModuleListener
    void on(ClientErased erased) {
        accounts.findByEmail(erased.email())
            .filter(account -> !account.role().isStaff())
            .ifPresent(accounts::delete);
        // A magic link already sent would otherwise create the account again at the next click.
        jdbc.update("DELETE FROM one_time_tokens WHERE username = ?", erased.email());
        sessions.findByPrincipalName(erased.email()).keySet().forEach(sessions::deleteById);
    }
}
