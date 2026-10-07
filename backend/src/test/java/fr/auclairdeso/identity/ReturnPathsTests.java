package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReturnPathsTests {

    @Test
    void keepsAPathOfThisSite() {
        var path = "/reserver/guidance-1-h?mode=VIDEO&debut=2026-10-12T14%3A00%3A00%2B02%3A00";

        assertThat(ReturnPaths.from(path)).contains(path);
    }

    @Test
    void refusesAnythingLeadingToAnotherSite() {
        assertThat(ReturnPaths.from("https://evil.example")).isEmpty();
        assertThat(ReturnPaths.from("//evil.example")).isEmpty();
        assertThat(ReturnPaths.from("/\\evil.example")).isEmpty();
    }

    @Test
    void ignoresAMissingValue() {
        assertThat(ReturnPaths.from(null)).isEmpty();
    }
}
