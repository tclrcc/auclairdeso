package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AttemptLimiterTests {

    private final AtomicLong nanos = new AtomicLong();
    private final AttemptLimiter limiter = new AttemptLimiter(2, Duration.ofMinutes(1), nanos::get);

    @Test
    void refusesAttemptsBeyondTheLimit() {
        assertThat(limiter.tryAcquire("key")).isTrue();
        assertThat(limiter.tryAcquire("key")).isTrue();
        assertThat(limiter.tryAcquire("key")).isFalse();
        assertThat(limiter.tryAcquire("other")).isTrue();
    }

    @Test
    void allowsAgainOnceTheWindowIsOver() {
        limiter.tryAcquire("key");
        limiter.tryAcquire("key");

        nanos.addAndGet(Duration.ofSeconds(61).toNanos());

        assertThat(limiter.tryAcquire("key")).isTrue();
    }
}
