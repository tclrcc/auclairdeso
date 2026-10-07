package fr.auclairdeso.booking;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class SlotController {

    static final int MAX_DAYS = 62;

    private final Availability availability;

    SlotController(Availability availability) {
        this.availability = availability;
    }

    @GetMapping("/offerings/{slug}/slots")
    List<TimeSlot> slots(@PathVariable String slug,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The period must go forward and last at most %d days".formatted(MAX_DAYS));
        }
        return availability.forOffering(slug, from, to)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active offering with slug '%s'".formatted(slug)));
    }
}
