package fr.auclairdeso.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** Appointments in the given statuses whose occupied time overlaps [from, to). */
    List<Appointment> findByStatusInAndStartsAtBeforeAndOccupiedUntilAfter(
        Collection<AppointmentStatus> statuses, Instant to, Instant from);

    boolean existsByClientAndStatusInAndStartsAtAfter(Client client, Collection<AppointmentStatus> statuses,
                                                      Instant after);

    List<Appointment> findByClientEmailOrderByStartsAtDesc(String email);

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(Instant from, Instant to);

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByStatusAndStartsAtAfterOrderByStartsAtAsc(AppointmentStatus status, Instant after);

    @EntityGraph(attributePaths = "client")
    Optional<Appointment> findWithClientById(Long id);
}
