package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClientDetailsTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    @Test
    void isAdultFromHerEighteenthBirthday() {
        assertThat(born(LocalDate.of(2008, 10, 11)).isAdultOn(TODAY)).isTrue();
    }

    @Test
    void isMinorTheDayBefore() {
        assertThat(born(LocalDate.of(2008, 10, 12)).isAdultOn(TODAY)).isFalse();
    }

    private static ClientDetails born(LocalDate birthDate) {
        return new ClientDetails("Alice", "Martin", birthDate, "06 12 34 56 78", null);
    }
}
