package fr.auclairdeso.catalog;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "offering")
class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 80, updatable = false)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_policy", nullable = false, length = 20)
    private PaymentPolicy paymentPolicy;

    @Column(name = "deposit_cents")
    private @Nullable Integer depositCents;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private OfferingCategory category;

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

    Offering(String slug, OfferingDraft draft) {
        this.slug = Objects.requireNonNull(slug, "slug");
        apply(draft);
    }

    void update(OfferingDraft draft) {
        apply(draft);
    }

    private void apply(OfferingDraft draft) {
        Objects.requireNonNull(draft, "draft");
        if (!draft.isDepositConsistent()) {
            throw new IllegalArgumentException("The deposit does not match the payment policy");
        }
        if (draft.modes() == null || draft.modes().isEmpty()) {
            throw new IllegalArgumentException("At least one consultation mode is required");
        }
        this.name = draft.name().strip();
        this.description = draft.description().strip();
        this.durationMinutes = draft.durationMinutes();
        this.bufferMinutes = draft.bufferMinutes();
        this.priceCents = draft.priceCents();
        this.paymentPolicy = Objects.requireNonNull(draft.paymentPolicy(), "paymentPolicy");
        this.depositCents = draft.depositCents();
        this.displayOrder = draft.displayOrder();
        this.active = draft.active();
        this.modes.clear();
        this.modes.addAll(draft.modes());
        this.category = Objects.requireNonNull(draft.category(), "category");
    }

    String slug() {
        return slug;
    }

    OfferingView toView() {
        return new OfferingView(slug, name, description, durationMinutes, bufferMinutes, priceCents,
            paymentPolicy, depositCents, sortedModes(), category);
    }

    OfferingDetails toDetails() {
        return new OfferingDetails(slug, name, description, durationMinutes, bufferMinutes, priceCents,
            paymentPolicy, depositCents, displayOrder, active, sortedModes(), category);
    }

    private java.util.List<ConsultationMode> sortedModes() {
        return modes.stream().sorted().toList();
    }
}
