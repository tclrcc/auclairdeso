package fr.auclairdeso.booking;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Times of day that drive the appointments without anyone clicking, bound from
 * "auclairdeso.booking.deadlines" (docs/specification.md, 4.3, 5.5 and 7).
 *
 * @param requestDeadline the day before the session, a request still unanswered at this time expires
 * @param reminderTime the day before the session, the reminder leaves from this time
 * @param bookingCutoff the day before the session, its slots are no longer offered from this time:
 *                      before the request deadline, so that the practitioner has time to answer
 */
@ConfigurationProperties("auclairdeso.booking.deadlines")
record BookingDeadlines(
    @DefaultValue("21:00") LocalTime requestDeadline,
    @DefaultValue("12:00") LocalTime reminderTime,
    @DefaultValue("19:00") LocalTime bookingCutoff) {

    /** Refuses to start with a cutoff at or after the deadline: a request could expire as soon as sent. */
    BookingDeadlines {
        if (!bookingCutoff.isBefore(requestDeadline)) {
            throw new IllegalArgumentException(
                "auclairdeso.booking.deadlines.booking-cutoff (" + bookingCutoff
                    + ") must be before auclairdeso.booking.deadlines.request-deadline (" + requestDeadline
                    + "), otherwise a request sent after the deadline would expire at once");
        }
    }
}
