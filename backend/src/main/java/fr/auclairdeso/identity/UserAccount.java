package fr.auclairdeso.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "user_account")
class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "password_hash", length = 255)
    private @Nullable String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccount() {
        // required by JPA
    }

    private UserAccount(String email, Role role) {
        this.email = Objects.requireNonNull(email, "email");
        this.role = Objects.requireNonNull(role, "role");
    }

    static UserAccount newClient(String email) {
        return new UserAccount(email, Role.CLIENT);
    }

    static UserAccount withRole(String email, Role role) {
        return new UserAccount(email, role);
    }

    void recordLogin(Instant at) {
        this.lastLoginAt = at;
    }

    void grant(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "newRole");
        if (!newRole.isStaff()) {
            this.passwordHash = null;
        }
    }

    void changePassword(String newPasswordHash) {
        if (!role.isStaff()) {
            throw new IllegalStateException("Only staff members have a password");
        }
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
    }

    void clearPassword() {
        this.passwordHash = null;
    }

    String email() {
        return email;
    }

    Role role() {
        return role;
    }

    @Nullable String passwordHash() {
        return passwordHash;
    }

    boolean hasPassword() {
        return passwordHash != null;
    }
}
