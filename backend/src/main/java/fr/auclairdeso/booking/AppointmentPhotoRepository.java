package fr.auclairdeso.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface AppointmentPhotoRepository extends JpaRepository<AppointmentPhoto, Long> {

    /** Which of these appointments have a photo, without loading any image. */
    @Query("select p.appointmentId from AppointmentPhoto p where p.appointmentId in :ids")
    Set<Long> findAppointmentIdsWithPhoto(Collection<Long> ids);

    /** The portraits of the sessions that ended before this instant, in one statement (9). */
    @Modifying(clearAutomatically = true)
    @Query("""
            delete from AppointmentPhoto p
            where p.appointmentId in (select a.id from Appointment a where a.endsAt < :before)
            """)
    int deleteBySessionEndedBefore(Instant before);

    int deleteByAppointmentIdIn(Collection<Long> appointmentIds);
}
