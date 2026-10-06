package fr.auclairdeso.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountPasswordController {

    private final StaffPasswords passwords;

    AccountPasswordController(StaffPasswords passwords) {
        this.passwords = passwords;
    }

    @PutMapping("/account/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void change(@Valid @RequestBody NewPassword request, Authentication authentication) {
        passwords.change(authentication, request.password());
    }

    record NewPassword(@NotNull String password) {
    }
}
