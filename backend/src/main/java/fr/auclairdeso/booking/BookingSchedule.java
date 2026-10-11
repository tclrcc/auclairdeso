package fr.auclairdeso.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * When the deadlines are checked. The cron expressions are read in the practitioner's time zone.
 */
@Component
class BookingSchedule {

    private static final Logger log = LoggerFactory.getLogger(BookingSchedule.class);

    private final AppointmentDeadlines deadlines;

    BookingSchedule(AppointmentDeadlines deadlines) {
        this.deadlines = deadlines;
    }

    /** Every quarter of an hour: two small queries, and nothing is missed after a restart. */
    @Scheduled(cron = "0 */15 * * * *", zone = "Europe/Paris")
    void everyQuarterOfAnHour() {
        var expired = deadlines.expireUnansweredRequests();
        var reminded = deadlines.sendReminders();
        if (expired > 0 || reminded > 0) {
            log.info("{} request(s) expired, {} reminder(s) sent", expired, reminded);
        }
    }

    @Scheduled(cron = "${auclairdeso.booking.deadlines.evening-program-cron:0 0 19 * * *}", zone = "Europe/Paris")
    void everyEvening() {
        if (deadlines.publishEveningProgram()) {
            log.info("Evening program sent to the practitioner");
        }
    }
}
