package fr.auclairdeso.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Who issues the bills, bound from "auclairdeso.billing" (docs/specification.md, 10).
 * The real values live in deploy/.env only: the address of the practitioner never goes into Git.
 * The sizes are those of the bill table: a value too long stops the application at startup,
 * instead of failing each completion.
 *
 * @param zone the time zone of the practitioner, the same as the booking module's
 * @param practitionerName her name, followed by "EI" (entrepreneur individuel)
 * @param address her professional address, on one line
 */
@Validated
@ConfigurationProperties("auclairdeso.billing")
record BillingProperties(
    @DefaultValue("Europe/Paris") ZoneId zone,
    @NotBlank @Size(max = 120) String practitionerName,
    @NotBlank @Size(max = 300) String address,
    @NotBlank @Size(max = 20) String siret) {
}
