package fr.auclairdeso.booking;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/appointments")
class AgendaController {

    private static final int MAX_DAYS = 62;

    private final Agenda agenda;

    AgendaController(Agenda agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/pending")
    List<AdminAppointmentView> pending() {
        return agenda.pending();
    }

    @GetMapping
    List<AdminAppointmentView> between(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The period must go forward and last at most %d days".formatted(MAX_DAYS));
        }
        return agenda.between(from, to);
    }

    @GetMapping("/{id}")
    AdminAppointmentView get(@PathVariable long id) {
        return agenda.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** The client's portrait: served only to staff, never kept in any cache. */
    @GetMapping("/{id}/photo")
    ResponseEntity<byte[]> photo(@PathVariable long id) {
        return agenda.photo(id)
            .map(photo -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(photo.data()))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/confirm")
    AdminAppointmentView confirm(@PathVariable long id) {
        return agenda.confirm(id);
    }

    @PostMapping("/{id}/decline")
    AdminAppointmentView decline(@PathVariable long id) {
        return agenda.decline(id);
    }

    @GetMapping("/to-close")
    List<AdminAppointmentView> toClose() {
        return agenda.toClose();
    }

    @PostMapping("/{id}/cancel")
    AdminAppointmentView cancel(@PathVariable long id) {
        return agenda.cancel(id);
    }

    @PostMapping("/{id}/complete")
    AdminAppointmentView complete(@PathVariable long id) {
        return agenda.complete(id);
    }

    @PostMapping("/{id}/no-show")
    AdminAppointmentView noShow(@PathVariable long id) {
        return agenda.noShow(id);
    }
}
