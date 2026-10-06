package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTests {

    @Test
    void keepsLettersAndDigitsSeparatedByDashes() {
        assertThat(Slugs.from("Guidance — 1 h")).isEqualTo("guidance-1-h");
    }

    @Test
    void removesAccentsAndExpandsLigatures() {
        assertThat(Slugs.from("Énergie & Harmonie")).isEqualTo("energie-harmonie");
        assertThat(Slugs.from("Cœur à cœur")).isEqualTo("coeur-a-coeur");
    }

    @Test
    void neverEndsWithADashWhenTruncated() {
        var slug = Slugs.from("séance ".repeat(20));

        assertThat(slug).hasSizeLessThanOrEqualTo(Slugs.MAX_LENGTH).doesNotEndWith("-");
    }

    @Test
    void fallsBackWhenNothingIsLeft() {
        assertThat(Slugs.from("✨ ✨")).isEqualTo("seance");
    }
}
