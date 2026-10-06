package fr.auclairdeso.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings, bound from the "auclairdeso" prefix.
 */
@Validated
@ConfigurationProperties("auclairdeso")
public record AppProperties(
    @NotNull URI frontendUrl,
    @NotBlank String mailFrom) {
}
