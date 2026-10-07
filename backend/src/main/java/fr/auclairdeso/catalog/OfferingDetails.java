package fr.auclairdeso.catalog;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Vue complète des détails d'une réservation pour l'admin
 */
record OfferingDetails(
    String slug,
    String name,
    String description,
    int durationMinutes,
    int bufferMinutes,
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
