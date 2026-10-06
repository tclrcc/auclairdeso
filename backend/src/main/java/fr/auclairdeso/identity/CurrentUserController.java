package fr.auclairdeso.identity;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CurrentUserController {

    private final UserAccountRepository accounts;

    CurrentUserController(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/me")
    CurrentUser me(Authentication authentication) {
        var authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        var passwordSet = accounts.findByEmail(authentication.getName())
            .map(UserAccount::hasPassword)
            .orElse(false);
        return new CurrentUser(
            authentication.getName(),
            withoutPrefix(authorities, "ROLE_"),
            withoutPrefix(authorities, "FACTOR_"),
            passwordSet);
    }

    private static List<String> withoutPrefix(List<String> authorities, String prefix) {
        return authorities.stream()
            .filter(authority -> authority.startsWith(prefix))
            .map(authority -> authority.substring(prefix.length()))
            .sorted()
            .toList();
    }
}
