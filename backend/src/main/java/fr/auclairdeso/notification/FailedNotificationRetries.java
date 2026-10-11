package fr.auclairdeso.notification;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * An email that could not leave (mail server unreachable, quota reached) is tried again later,
 * a dozen times at most, so that a hopeless one does not come back forever.
 */
@Component
class FailedNotificationRetries {

    private static final int MAX_ATTEMPTS = 12;

    private final FailedEventPublications failed;

    FailedNotificationRetries(FailedEventPublications failed) {
        this.failed = failed;
    }

    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void retry() {
        failed.resubmit(ResubmissionOptions.defaults()
            .withMinAge(Duration.ofMinutes(5))
            .withFilter(publication -> publication.getCompletionAttempts() < MAX_ATTEMPTS));
    }
}
