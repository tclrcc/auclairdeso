package fr.auclairdeso.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/offerings")
class OfferingController {

    private final OfferingCatalog catalog;

    OfferingController(OfferingCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<OfferingView> list() {
        return catalog.findActiveOfferings();
    }

    @GetMapping("/{slug}")
    OfferingView get(@PathVariable String slug) {
        return catalog.findActiveOffering(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active offering with slug '%s'".formatted(slug)));
    }
}
