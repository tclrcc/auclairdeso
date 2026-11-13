package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** The cutoff must fall before the deadline, or the application refuses to start. */
class BookingDeadlinesTests {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BookingDeadlines.class)
    static class WithDeadlines {
    }

    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(WithDeadlines.class);

    @Test
    void theDefaultsCloseTomorrowAtSevenTwoHoursBeforeTheDeadline() {
        context.run(started -> assertThat(started.getBean(BookingDeadlines.class))
            .returns(LocalTime.of(19, 0), BookingDeadlines::bookingCutoff)
            .returns(LocalTime.of(21, 0), BookingDeadlines::requestDeadline));
    }

    @Test
    void aCutoffAfterTheDeadlineStopsTheStart() {
        context.withPropertyValues("auclairdeso.booking.deadlines.booking-cutoff=21:30")
            .run(started -> assertThat(started).hasFailed()
                .getFailure().rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("booking-cutoff (21:30) must be before")
                .hasMessageContaining("request-deadline (21:00)"));
    }

    @Test
    void aCutoffAtTheDeadlineStopsTheStartToo() {
        context.withPropertyValues("auclairdeso.booking.deadlines.booking-cutoff=21:00")
            .run(started -> assertThat(started).hasFailed());
    }
}
