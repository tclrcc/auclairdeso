package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ClosureAdministration {

    private final ClosureRepository closures;
    private final BookingRules rules;
    private final Clock clock;

    ClosureAdministration(ClosureRepository closures, BookingRules rules, Clock clock) {
        this.closures = closures;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    List<ClosureView> upcoming() {
        var today = LocalDate.now(clock.withZone(rules.zone()));
        return closures.findByLastDayGreaterThanEqualOrderByFirstDayAsc(today).stream()
            .map(Closure::toView)
            .toList();
    }

    ClosureView create(ClosureRequest request) {
        return closures.save(new Closure(request.firstDay(), request.lastDay(), request.reason())).toView();
    }

    boolean delete(long id) {
        if (!closures.existsById(id)) {
            return false;
        }
        closures.deleteById(id);
        return true;
    }
}
