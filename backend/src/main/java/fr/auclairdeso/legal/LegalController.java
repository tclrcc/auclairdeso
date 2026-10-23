package fr.auclairdeso.legal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The identity of the publisher, read by the legal pages of the site.
 * <p>
 * It is served rather than written into the pages so that the practitioner's address, which is still
 * being settled, can be changed in deploy/.env without touching the code.
 */
@RestController
class LegalController {

    private final LegalIdentity identity;

    LegalController(LegalProperties properties) {
        this.identity = properties.toIdentity();
    }

    @GetMapping("/legal")
    LegalIdentity legal() {
        return identity;
    }
}
