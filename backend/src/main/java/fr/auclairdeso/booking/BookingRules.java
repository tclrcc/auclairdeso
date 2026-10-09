package fr.auclairdeso.booking;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Booking rules of the practitioner, bound from "auclairdeso.booking" (docs/specification.md, 4.3 and 6).
 */
@ConfigurationProperties("auclairdeso.booking")
record BookingRules(
    @DefaultValue("Europe/Paris") ZoneId zone,
    @DefaultValue("3") int maxSessionsPerDay,
    @DefaultValue("1") int minDaysAhead,
    @DefaultValue("2m") Period horizon,
    @DefaultValue("48h") Duration cancellationNotice,
    @DefaultValue("2") int lateCancellationsBeforeBlock) {
}
