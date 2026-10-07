package fr.auclairdeso.booking;

import org.springframework.data.jpa.repository.JpaRepository;

interface AppointmentPhotoRepository extends JpaRepository<AppointmentPhoto, Long> {
}
