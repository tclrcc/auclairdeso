package fr.auclairdeso.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Offering management for the practitioner: every offering, visible or hidden.
 */
@Service
@Transactional
class OfferingAdministration {

    private final OfferingRepository offerings;

    OfferingAdministration(OfferingRepository offerings) {
        this.offerings = offerings;
    }

    @Transactional(readOnly = true)
    List<OfferingDetails> findAll() {
        return offerings.findAllByOrderByDisplayOrderAscNameAsc().stream()
            .map(Offering::toDetails)
            .toList();
    }

    @Transactional(readOnly = true)
    Optional<OfferingDetails> find(String slug) {
        return offerings.findBySlug(slug).map(Offering::toDetails);
    }

    OfferingDetails create(OfferingDraft draft) {
        var slug = Slugs.from(draft.name());
        if (offerings.existsBySlug(slug)) {
            throw new OfferingNameTakenException(slug);
        }
        return offerings.save(new Offering(slug, draft)).toDetails();
    }

    Optional<OfferingDetails> update(String slug, OfferingDraft draft) {
        return offerings.findBySlug(slug).map(offering -> {
            offering.update(draft);
            return offering.toDetails();
        });
    }
}
