package fr.auclairdeso.legal;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Who publishes the site, for the legal pages (docs/specification.md, 10).
 * <p>
 * The identity of the practitioner comes from the same values as the bills: one place to fill in
 * deploy/.env, and her address never goes into Git.
 *
 * @param publisherName her name, followed by "EI" (entrepreneur individuel)
 * @param address her professional address, on one line
 * @param contactEmail the address clients write to about their data; the practitioner's own
 * @param mediatorName the consumer mediator, required to sell to individuals; empty until one is chosen
 * @param mediatorUrl where to refer a dispute to that mediator
 */
@Validated
@ConfigurationProperties("auclairdeso.legal")
record LegalProperties(
    @NotBlank String publisherName,
    @NotBlank String address,
    @NotBlank String siret,
    @NotBlank String contactEmail,
    @NotBlank String host,
    @DefaultValue("") String mediatorName,
    @DefaultValue("") String mediatorUrl) {

    LegalIdentity toIdentity() {
        return new LegalIdentity(publisherName, address, siret, contactEmail, host,
            blankToNull(mediatorName), blankToNull(mediatorUrl));
    }

    private static @Nullable String blankToNull(String value) {
        return value.isBlank() ? null : value.strip();
    }
}
