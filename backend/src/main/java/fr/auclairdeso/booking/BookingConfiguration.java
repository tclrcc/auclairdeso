package fr.auclairdeso.booking;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BookingConfiguration {

    @Bean
    SlotCalculator slotCalculator(BookingRules rules, Clock clock) {
        return new SlotCalculator(rules, clock);
    }
}
