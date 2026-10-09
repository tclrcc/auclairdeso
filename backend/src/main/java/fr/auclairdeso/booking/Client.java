package fr.auclairdeso.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "client")
class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 254)
    private @Nullable String email;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "birth_date")
    private @Nullable LocalDate birthDate;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "city", length = 100)
    private @Nullable String city;

    @Column(name = "trusted", nullable = false)
    private boolean trusted;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Client() {
        // required by JPA
    }

    /** A client who books online: her email comes from her login. */
    Client(String email) {
        this.email = Objects.requireNonNull(email, "email");
    }

    /** A client entered by the practitioner, e.g. after a phone call: email and birth date are optional. */
    static Client byPractitioner(ManualClient details, @Nullable String email) {
        var client = new Client();
        client.email = email;
        client.firstName = details.firstName().strip();
        client.lastName = details.lastName().strip();
        client.phone = details.phone().strip();
        client.city = blankToNull(details.city());
        return client;
    }

    /** The client may correct her details at each online booking. */
    void update(ClientDetails details) {
        this.firstName = details.firstName().strip();
        this.lastName = details.lastName().strip();
        this.birthDate = details.birthDate();
        this.phone = details.phone().strip();
        this.city = blankToNull(details.city());
    }

    void block() {
        this.blocked = true;
    }

    @Nullable Long id() {
        return id;
    }

    boolean isTrusted() {
        return trusted;
    }

    boolean isBlocked() {
        return blocked;
    }

    ClientProfile toProfile() {
        return new ClientProfile(firstName, lastName, birthDate, phone, city, trusted);
    }

    ClientSummary toSummary() {
        return new ClientSummary(id, email, firstName, lastName, birthDate, phone, city, trusted, blocked);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
