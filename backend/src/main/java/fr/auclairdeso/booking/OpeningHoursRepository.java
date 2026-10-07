package fr.auclairdeso.booking;

import org.springframework.data.jpa.repository.JpaRepository;

interface OpeningHoursRepository extends JpaRepository<OpeningHours, Long> {
}
