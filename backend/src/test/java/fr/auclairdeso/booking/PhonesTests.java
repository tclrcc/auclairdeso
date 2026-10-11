package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhonesTests {

    @Test
    void theSameNumberWrittenInAnyWay() {
        assertThat(Phones.normalize("06 12 34 56 78")).isEqualTo("0612345678");
        assertThat(Phones.normalize("06.12.34.56.78")).isEqualTo("0612345678");
        assertThat(Phones.normalize("+33 6 12 34 56 78")).isEqualTo("0612345678");
        assertThat(Phones.normalize("+33 06 12 34 56 78")).isEqualTo("0612345678");
        assertThat(Phones.normalize("0033 6 12 34 56 78")).isEqualTo("0612345678");
    }

    @Test
    void onlyTheSameNumberIsTheSame() {
        assertThat(Phones.same("06 12 34 56 78", "+33612345678")).isTrue();
        assertThat(Phones.same("06 12 34 56 78", "06 12 34 56 79")).isFalse();
    }
}
