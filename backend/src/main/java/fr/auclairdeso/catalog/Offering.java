package fr.auclairdeso.catalog;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "offering")
class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_policy", nullable = false, length = 20)
    private PaymentPolicy paymentPolicy;

    @Column(name = "deposit_cents")
    private Integer depositCents;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "offering_mode", joinColumns = @JoinColumn(name = "offering_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private Set<ConsultationMode> modes = EnumSet.noneOf(ConsultationMode.class);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Offering() {
        // required by JPA
    }

    Offering(String slug, String name, String description, int durationMinutes, int priceCents,
             PaymentPolicy paymentPolicy, Integer depositCents, int displayOrder,
             Set<ConsultationMode> modes) {
        Objects.requireNonNull(paymentPolicy, "paymentPolicy");
        Objects.requireNonNull(modes, "modes");
        if (paymentPolicy == PaymentPolicy.DEPOSIT_ONLINE) {
            if (depositCents == null || depositCents < 1 || depositCents > priceCents) {
                throw new IllegalArgumentException("A deposit between 1 cent and the price is required");
            }
        } else if (depositCents != null) {
            throw new IllegalArgumentException("A deposit is only allowed with the DEPOSIT_ONLINE policy");
        }
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("At least one consultation mode is required");
        }
        this.slug = Objects.requireNonNull(slug, "slug");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.durationMinutes = durationMinutes;
        this.priceCents = priceCents;
        this.paymentPolicy = paymentPolicy;
        this.depositCents = depositCents;
        this.displayOrder = displayOrder;
        this.modes = EnumSet.copyOf(modes);
    }

    void deactivate() {
        this.active = false;
    }

    OfferingView toView() {
        return new OfferingView(slug, name, description, durationMinutes, priceCents,
            paymentPolicy, depositCents, modes.stream().sorted().toList());
    }
}
