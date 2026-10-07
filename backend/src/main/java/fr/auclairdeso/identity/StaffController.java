package fr.auclairdeso.identity;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StaffController {

    private final UserAccountRepository accounts;
    private final StaffPasswords passwords;

    StaffController(UserAccountRepository accounts, StaffPasswords passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    @GetMapping("/admin/staff")
    List<StaffMember> staff() {
        return accounts.findByRoleInOrderByEmailAsc(List.of(Role.PRACTITIONER, Role.ADMIN)).stream()
            .map(account -> new StaffMember(account.email(), account.role(), account.hasPassword()))
            .toList();
    }

    @DeleteMapping("/admin/staff/{email}/password")
    ResponseEntity<Void> resetPassword(@PathVariable String email) {
        return passwords.reset(email)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    record StaffMember(String email, Role role, boolean passwordSet) {
    }
}
