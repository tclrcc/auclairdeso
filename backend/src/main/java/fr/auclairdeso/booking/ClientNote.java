package fr.auclairdeso.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;

/** A private note of the practitioner about a client (docs/specification.md, 8). Never shown to the client. */
@Entity
@Table(name = "client_note")
class ClientNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "text", nullable = false, updatable = false, columnDefinition = "text")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClientNote() {
        // required by JPA
    }

    ClientNote(Client client, String text, Instant createdAt) {
        this.client = client;
        this.text = text.strip();
        this.createdAt = createdAt;
    }

    ClientNoteView toView(ZoneId zone) {
        return new ClientNoteView(id, text, createdAt.atZone(zone).toOffsetDateTime());
    }
}
