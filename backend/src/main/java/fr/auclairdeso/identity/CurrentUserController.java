package fr.auclairdeso.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/me")
class CurrentUserController {

    private static final String ROLE_PREFIX = "ROLE_";

    @GetMapping
    CurrentUser me(Authentication authentication) {
        var roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
            .filter(authority -> authority.startsWith(ROLE_PREFIX))
            .map(authority -> authority.substring(ROLE_PREFIX.length()))
            .sorted()
            .toList();
        return new CurrentUser(authentication.getName(), roles);
    }
}
