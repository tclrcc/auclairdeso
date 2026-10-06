package fr.auclairdeso.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OfferingCatalog {

    private final OfferingRepository offerings;

    OfferingCatalog(OfferingRepository offerings) {
        this.offerings = offerings;
    }

    public List<OfferingView> findActiveOfferings() {
        return offerings.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
            .map(Offering::toView)
            .toList();
    }

    public Optional<OfferingView> findActiveOffering(String slug) {
        return offerings.findBySlugAndActiveTrue(slug).map(Offering::toView);
    }
}
