package fr.auclairdeso.booking;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AppointmentPhotoRepository extends JpaRepository<AppointmentPhoto, Long> {

    /** Which of these appointments have a photo, without loading any image. */
    @Query("select p.appointmentId from AppointmentPhoto p where p.appointmentId in :ids")
    Set<Long> findAppointmentIdsWithPhoto(Collection<Long> ids);
}
