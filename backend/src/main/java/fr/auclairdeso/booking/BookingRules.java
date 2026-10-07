package fr.auclairdeso.booking;

import java.time.Period;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("auclairdeso.booking")
record BookingRules(
    @DefaultValue("Europe/Paris") ZoneId zone,
    @DefaultValue("3") int maxSessionsPerDay,
    @DefaultValue("1") int minDaysAhead,
    @DefaultValue("2m") Period horizon) {
}
