package fr.auclairdeso.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "appointment_photo")
class AppointmentPhoto {

    @Id
    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected AppointmentPhoto() {
        // required by JPA
    }

    AppointmentPhoto(Long appointmentId, Photo photo) {
        this.appointmentId = appointmentId;
        this.contentType = photo.contentType();
        this.data = photo.data();
    }

    String contentType() {
        return contentType;
    }

    byte[] data() {
        return data;
    }
}
