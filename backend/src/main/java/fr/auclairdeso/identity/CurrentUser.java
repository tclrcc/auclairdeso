package fr.auclairdeso.identity;

import java.util.List;

public record CurrentUser(String email, List<String> roles) {

    public CurrentUser {
        roles = List.copyOf(roles);
    }
}
