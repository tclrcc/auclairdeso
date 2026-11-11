package fr.auclairdeso.catalog;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record OfferingView(
    String slug,
    String name,
    String description,
    int durationMinutes,
    int bufferMinutes,
    int priceCents,
    PaymentPolicy paymentPolicy,
    @Nullable Integer depositCents,
    List<ConsultationMode> modes,
    OfferingCategory category,
    @Nullable String videoPath,
    @Nullable String videoTranscript) {

    public OfferingView {
        modes = List.copyOf(modes);
    }
}
