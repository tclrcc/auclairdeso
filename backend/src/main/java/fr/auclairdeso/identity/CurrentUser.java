package fr.auclairdeso.identity;

import java.util.List;

/**
 * The logged-in user, as seen by the frontend.
 *
 * @param factors how the user proved their identity in this session: OTT (magic link), PASSWORD
 * @param passwordSet whether a staff member has already defined their password
 */
public record CurrentUser(String email, List<String> roles, List<String> factors, boolean passwordSet) {
}
