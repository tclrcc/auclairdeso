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
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "closure")
class Closure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_day", nullable = false)
    private LocalDate firstDay;

    @Column(name = "last_day", nullable = false)
    private LocalDate lastDay;

    @Column(name = "reason", length = 200)
    private @Nullable String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Closure() {
        // required by JPA
    }

    Closure(LocalDate firstDay, LocalDate lastDay, @Nullable String reason) {
        this.firstDay = Objects.requireNonNull(firstDay, "firstDay");
        this.lastDay = Objects.requireNonNull(lastDay, "lastDay");
        if (lastDay.isBefore(firstDay)) {
            throw new IllegalArgumentException("A closure cannot end before it starts");
        }
        this.reason = reason == null || reason.isBlank() ? null : reason.strip();
    }

    ClosedPeriod toPeriod() {
        return new ClosedPeriod(firstDay, lastDay);
    }

    ClosureView toView() {
        return new ClosureView(id, firstDay, lastDay, reason);
    }
}
