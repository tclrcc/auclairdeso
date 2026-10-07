package fr.auclairdeso.identity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts attempts per key over a fixed window that starts with the first attempt.
 */
final class AttemptLimiter {

    private final int maxAttempts;
    private final Cache<String, AtomicInteger> attempts;

    AttemptLimiter(int maxAttempts, Duration window) {
        this(maxAttempts, window, Ticker.systemTicker());
    }

    AttemptLimiter(int maxAttempts, Duration window, Ticker ticker) {
        this.maxAttempts = maxAttempts;
        this.attempts = Caffeine.newBuilder()
            .expireAfterWrite(window)
            .maximumSize(100_000)
            .ticker(ticker)
            .build();
    }

    boolean tryAcquire(String key) {
        return attempts.get(key, ignored -> new AtomicInteger()).incrementAndGet() <= maxAttempts;
    }
}
