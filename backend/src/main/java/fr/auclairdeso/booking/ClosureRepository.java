package fr.auclairdeso.booking;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClosureRepository extends JpaRepository<Closure, Long> {

    /** Closures overlapping [from, to]. */
    List<Closure> findByLastDayGreaterThanEqualAndFirstDayLessThanEqual(LocalDate from, LocalDate to);

    List<Closure> findByLastDayGreaterThanEqualOrderByFirstDayAsc(LocalDate day);
}
