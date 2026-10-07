package fr.auclairdeso.catalog;

import java.util.List;

public record OfferingView(
    String slug,
    String name,
    String description,
    int durationMinutes,
    int bufferMinutes,
    int priceCents,
    PaymentPolicy paymentPolicy,
    Integer depositCents,
    List<ConsultationMode> modes,
    OfferingCategory category) {

    public OfferingView {
        modes = List.copyOf(modes);
    }
}
