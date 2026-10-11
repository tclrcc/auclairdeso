package fr.auclairdeso.notification;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Practical details given to clients in their emails, bound from "auclairdeso.notification".
 * Never shown on the site: only in the confirmation and the reminder (docs/specification.md, 3.4).
 *
 * @param practitionerPhone the number clients call for a session by phone
 * @param practiceAddress where in-person sessions take place
 */
@ConfigurationProperties("auclairdeso.notification")
record NotificationProperties(@Nullable String practitionerPhone, @Nullable String practiceAddress) {
}
