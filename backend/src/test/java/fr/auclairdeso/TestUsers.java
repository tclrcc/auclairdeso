package fr.auclairdeso;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestUsers {

    private TestUsers() {
    }

    /** A practitioner who logged in with the magic link and the password. */
    public static RequestPostProcessor practitioner() {
        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_PRACTITIONER"),
            FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.OTT_AUTHORITY),
            FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));
        return authentication(UsernamePasswordAuthenticationToken.authenticated("so@example.com", null, authorities));
    }
}
