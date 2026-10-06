package fr.auclairdeso.identity;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Staff email addresses, bound from "auclairdeso.identity".
 */
@ConfigurationProperties("auclairdeso.identity")
record IdentityProperties(
    @DefaultValue List<String> admins,
    @DefaultValue List<String> practitioners) {
}
