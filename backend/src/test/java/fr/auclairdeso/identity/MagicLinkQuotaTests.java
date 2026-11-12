package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class MagicLinkQuotaTests {

    private final AtomicLong nanos = new AtomicLong();
    private final MagicLinkQuota quota = new MagicLinkQuota(Set.of("so@example.com"), nanos::get);

    @Test
    void allowsThreeLinksPerAddressEveryFifteenMinutes() {
        for (var i = 0; i < 3; i++) {
            assertThat(quota.allows("client@example.com")).isTrue();
        }
        assertThat(quota.allows("client@example.com")).isFalse();
        assertThat(quota.allows("other@example.com")).isTrue();

        pass(Duration.ofMinutes(16));

        assertThat(quota.allows("client@example.com")).isTrue();
    }

    @Test
    void allowsTenLinksPerAddressADay() {
        for (var window = 0; window < 4; window++) {
            for (var i = 0; i < 3; i++) {
                quota.allows("client@example.com");
            }
            pass(Duration.ofMinutes(16));
        }
        // Twelve asked over an hour: only ten were allowed, and none more until the day is over.
        assertThat(quota.allows("client@example.com")).isFalse();

        pass(Duration.ofDays(1));

        assertThat(quota.allows("client@example.com")).isTrue();
    }

    @Test
    void stopsAllLinksOnceTheDailyTotalIsReached() {
        for (var i = 0; i < MagicLinkQuota.DAILY_TOTAL; i++) {
            assertThat(quota.allows("client" + i + "@example.com")).isTrue();
        }

        assertThat(quota.allows("late@example.com")).isFalse();
        // Staff are left out of the total, so that such a flood cannot lock them out.
        assertThat(quota.allows("so@example.com")).isTrue();
    }

    @Test
    void staffStayLimitedPerAddress() {
        for (var i = 0; i < 3; i++) {
            quota.allows("so@example.com");
        }

        assertThat(quota.allows("so@example.com")).isFalse();
    }

    @Test
    void logsARefusalWithoutTheAddress(CapturedOutput output) {
        for (var i = 0; i < 4; i++) {
            quota.allows("secret.client@example.com");
        }

        assertThat(output).contains("WARN").contains("Magic link not sent: per-address limit reached");
        assertThat(output).doesNotContain("secret.client");
    }

    private void pass(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
