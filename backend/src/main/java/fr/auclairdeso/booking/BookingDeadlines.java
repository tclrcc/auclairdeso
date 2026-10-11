package fr.auclairdeso.booking;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Times of day that drive the appointments without anyone clicking, bound from
 * "auclairdeso.booking.deadlines" (docs/specification.md, 5.5 and 7).
 *
 * @param requestDeadline the day before the session, a request still unanswered at this time expires
 * @param reminderTime the day before the session, the reminder leaves from this time
 */
@ConfigurationProperties("auclairdeso.booking.deadlines")
record BookingDeadlines(
    @DefaultValue("21:00") LocalTime requestDeadline,
    @DefaultValue("12:00") LocalTime reminderTime) {
}
