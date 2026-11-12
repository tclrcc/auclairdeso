package fr.auclairdeso.identity;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * How many magic links may be emailed: anyone can ask for one, for any address, so without a ceiling
 * the site could flood an inbox or use up the sending quota of the mail account, which every other
 * notification shares.
 *
 * <p>A refused link is simply not sent. The caller gets the same answer as for a sent one, so the
 * answer never tells anything about an address. The counters live in memory: a restart of the
 * backend resets them, which is also how to unblock a member of staff at once.
 */
@Component
class MagicLinkQuota {

    static final int PER_ADDRESS = 3;
    static final Duration PER_ADDRESS_WINDOW = Duration.ofMinutes(15);
    static final int PER_ADDRESS_DAILY = 10;
    static final int DAILY_TOTAL = 100;

    private static final Logger log = LoggerFactory.getLogger(MagicLinkQuota.class);
    private static final Duration DAY = Duration.ofDays(1);
    private static final String ALL_ADDRESSES = "*";

    private final Set<String> staff;
    private final AttemptLimiter perAddress;
    private final AttemptLimiter perAddressDaily;
    private final AttemptLimiter total;

    @Autowired
    MagicLinkQuota(IdentityProperties properties) {
        this(staffEmails(properties), Ticker.systemTicker());
    }

    MagicLinkQuota(Set<String> staff, Ticker ticker) {
        this.staff = Set.copyOf(staff);
        this.perAddress = new AttemptLimiter(PER_ADDRESS, PER_ADDRESS_WINDOW, ticker);
        this.perAddressDaily = new AttemptLimiter(PER_ADDRESS_DAILY, DAY, ticker);
        this.total = new AttemptLimiter(DAILY_TOTAL, DAY, ticker);
    }

    /**
     * Whether a link may be emailed to this normalized address now, and if so, counts it.
     * Never logs the address: a refusal only says which ceiling was reached.
     */
    boolean allows(String email) {
        if (!perAddress.tryAcquire(email) || !perAddressDaily.tryAcquire(email)) {
            log.warn("Magic link not sent: per-address limit reached");
            return false;
        }
        // Staff are left out of the total: a flood aimed at every address must not lock them out.
        if (!staff.contains(email) && !total.tryAcquire(ALL_ADDRESSES)) {
            log.warn("Magic link not sent: daily limit of {} links reached", DAILY_TOTAL);
            return false;
        }
        return true;
    }

    private static Set<String> staffEmails(IdentityProperties properties) {
        return Stream.concat(properties.admins().stream(), properties.practitioners().stream())
            .map(EmailAddresses::normalize)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }
}
