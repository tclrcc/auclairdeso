package fr.auclairdeso.shared;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the @Scheduled methods (reminders, expirations, evening program, retries).
 * A test sets auclairdeso.scheduling.enabled=false to run them itself, at the time of its own clock.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "auclairdeso.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfiguration {
}
