package fr.auclairdeso.billing;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    /**
     * The bill, locked until the end of the transaction: a double tap on "Enregistrer le paiement"
     * makes the second request wait, then fail with "déjà encaissée", instead of writing twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Bill> findForUpdateById(Long id);

    List<Bill> findByAppointmentId(Long appointmentId);

    /** Bills waiting for their payment, the oldest first. */
    List<Bill> findByPaidOnIsNullOrderByIssuedOnAscNumberAsc();

    /** Payments of a period, in the order of the receipt book. */
    List<Bill> findByPaidOnBetweenOrderByPaidOnAscNumberAsc(LocalDate from, LocalDate to);
}
