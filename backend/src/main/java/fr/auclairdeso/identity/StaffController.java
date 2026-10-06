package fr.auclairdeso.identity;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StaffController {

    private final UserAccountRepository accounts;

    StaffController(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/admin/staff")
    List<StaffMember> staff() {
        return accounts.findByRoleInOrderByEmailAsc(List.of(Role.PRACTITIONER, Role.ADMIN)).stream()
            .map(account -> new StaffMember(account.email(), account.role(), account.hasPassword()))
            .toList();
    }

    record StaffMember(String email, Role role, boolean passwordSet) {
    }
}
