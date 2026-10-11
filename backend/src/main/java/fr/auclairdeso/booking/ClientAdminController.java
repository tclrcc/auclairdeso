package fr.auclairdeso.booking;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/clients")
class ClientAdminController {

    private final ClientDirectory directory;
    private final ClientFiles files;

    ClientAdminController(ClientDirectory directory, ClientFiles files) {
        this.directory = directory;
        this.files = files;
    }

    @GetMapping
    List<ClientSummary> search(@RequestParam(defaultValue = "") String query,
                               @RequestParam(defaultValue = "ALL") ClientFilter filter) {
        return directory.search(query, filter);
    }

    @GetMapping("/{id}")
    ClientFile file(@PathVariable long id) {
        return files.file(id);
    }

    @PutMapping("/{id}")
    ClientFile correct(@PathVariable long id, @Valid @RequestBody ClientCorrection correction) {
        return files.correct(id, correction);
    }

    @PostMapping("/{id}/trust")
    ClientFile trust(@PathVariable long id) {
        return files.trust(id, true);
    }

    @PostMapping("/{id}/distrust")
    ClientFile distrust(@PathVariable long id) {
        return files.trust(id, false);
    }

    @PostMapping("/{id}/block")
    ClientFile block(@PathVariable long id) {
        return files.block(id);
    }

    @PostMapping("/{id}/unblock")
    ClientFile unblock(@PathVariable long id) {
        return files.unblock(id);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    ClientNoteView addNote(@PathVariable long id, @Valid @RequestBody NoteRequest note) {
        return files.addNote(id, note);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteNote(@PathVariable long id, @PathVariable long noteId) {
        files.deleteNote(id, noteId);
    }
}
