package fr.auclairdeso.catalog;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/admin/offerings")
class OfferingAdminController {

    private final OfferingAdministration administration;

    OfferingAdminController(OfferingAdministration administration) {
        this.administration = administration;
    }

    @GetMapping
    List<OfferingDetails> list() {
        return administration.findAll();
    }

    @GetMapping("/{slug}")
    OfferingDetails get(@PathVariable String slug) {
        return administration.find(slug).orElseThrow(() -> notFound(slug));
    }

    @PostMapping
    ResponseEntity<OfferingDetails> create(@Valid @RequestBody OfferingDraft draft) {
        var created = administration.create(draft);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{slug}")
            .buildAndExpand(created.slug())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{slug}")
    OfferingDetails update(@PathVariable String slug, @Valid @RequestBody OfferingDraft draft) {
        return administration.update(slug, draft).orElseThrow(() -> notFound(slug));
    }

    private static ResponseStatusException notFound(String slug) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No offering with slug '%s'".formatted(slug));
    }
}
