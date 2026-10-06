package fr.auclairdeso.catalog;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Full view of an offering for the admin area, including hidden ones. Amounts are in euro cents.
 */
record OfferingDetails(
    String slug,
    String name,
    String description,
    int durationMinutes,
    int priceCents,
    PaymentPolicy paymentPolicy,
    @Nullable Integer depositCents,
    int displayOrder,
    boolean active,
    List<ConsultationMode> modes) {

    OfferingDetails {
        modes = List.copyOf(modes);
    }
}
