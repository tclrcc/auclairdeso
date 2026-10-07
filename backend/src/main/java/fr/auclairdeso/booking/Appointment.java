package fr.auclairdeso.booking;

import fr.auclairdeso.catalog.ConsultationMode;
import fr.auclairdeso.catalog.OfferingView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "appointment")
class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "offering_slug", nullable = false, length = 80, updatable = false)
    private String offeringSlug;

    @Column(name = "offering_name", nullable = false, length = 120)
    private String offeringName;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private ConsultationMode mode;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "occupied_until", nullable = false)
    private Instant occupiedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "address", length = 300)
    private @Nullable String address;

    @Column(name = "messenger_name", length = 100)
    private @Nullable String messengerName;

    @Column(name = "consented_at", nullable = false, updatable = false)
    private Instant consentedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
        // required by JPA
    }

    /** A new request, waiting for the practitioner's decision. Name and price are copied from the offering. */
    static Appointment request(Client client, OfferingView offering, BookingRequest request, Instant now) {
        var appointment = new Appointment();
        appointment.client = client;
        appointment.offeringSlug = offering.slug();
        appointment.offeringName = offering.name();
        appointment.priceCents = offering.priceCents();
        appointment.mode = request.mode();
        appointment.startsAt = request.start().toInstant();
        appointment.endsAt = appointment.startsAt.plus(Duration.ofMinutes(offering.durationMinutes()));
        appointment.occupiedUntil = appointment.endsAt.plus(Duration.ofMinutes(offering.bufferMinutes()));
        appointment.status = AppointmentStatus.REQUESTED;
        appointment.reason = request.reason().strip();
        appointment.address = request.mode() == ConsultationMode.CLIENT_HOME ? blankToNull(request.address()) : null;
        appointment.messengerName = request.mode() == ConsultationMode.VIDEO ? blankToNull(request.messengerName()) : null;
        appointment.consentedAt = now;
        return appointment;
    }

    Long id() {
        return id;
    }

    void confirm() {
        requireStatus(AppointmentStatus.REQUESTED, "Seule une demande en attente peut être confirmée.");
        this.status = AppointmentStatus.CONFIRMED;
    }

    void decline() {
        requireStatus(AppointmentStatus.REQUESTED, "Seule une demande en attente peut être refusée.");
        this.status = AppointmentStatus.DECLINED;
    }

    AppointmentStatus status() {
        return status;
    }

    AdminAppointmentView toAdminView(ZoneId zone, boolean hasPhoto) {
        return new AdminAppointmentView(id, offeringSlug, offeringName, mode,
            startsAt.atZone(zone).toOffsetDateTime(), endsAt.atZone(zone).toOffsetDateTime(),
            status, priceCents, reason, address, messengerName, hasPhoto, client.toSummary());
    }

    private void requireStatus(AppointmentStatus expected, String message) {
        if (status != expected) {
            throw new BookingRefusedException(HttpStatus.CONFLICT, message);
        }
    }

    BookedSession toBookedSession(ZoneId zone) {
        return new BookedSession(startsAt.atZone(zone),
            Duration.between(startsAt, endsAt), Duration.between(endsAt, occupiedUntil));
    }

    AppointmentView toView(ZoneId zone) {
        return new AppointmentView(id, offeringSlug, offeringName, mode,
            startsAt.atZone(zone).toOffsetDateTime(), endsAt.atZone(zone).toOffsetDateTime(),
            status, priceCents);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
