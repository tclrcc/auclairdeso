package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

import static fr.auclairdeso.TestcontainersConfiguration.NO_MAIL_HEALTH_CHECK;

/**
 * With the "dev" profile, Flyway loads db/dev/R__dev_offerings.sql: the public API then lists the four offerings
 * of the catalogue agreed with the practitioner, and nothing else.
 */
@SpringBootTest(properties = NO_MAIL_HEALTH_CHECK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DevOfferingsSeedTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void thePublicApiListsTheFourOfferingsOfTheCatalogue() {
        var json = mvc.get().uri("/api/offerings");

        assertThat(json).hasStatusOk().bodyJson().extractingPath("$[*].slug").asArray()
            .containsExactly("guidance-1-h", "guidance-30-min", "magnetisme", "reequilibrage-energetique");
        assertThat(json).bodyJson().extractingPath("$[*].priceCents").asArray()
            .containsExactly(8_000, 5_000, 6_000, 7_000);
        assertThat(json).bodyJson().extractingPath("$[*].durationMinutes").asArray()
            .containsExactly(60, 30, 45, 60);
        assertThat(json).bodyJson().extractingPath("$[*].category").asArray()
            .containsExactly("CLAIRVOYANCE", "CLAIRVOYANCE", "MAGNETISM", "ENERGY_REBALANCING");
    }

    @Test
    void everyOfferingIsPaidOnSiteWithoutDeposit() {
        var json = mvc.get().uri("/api/offerings");

        assertThat(json).bodyJson().extractingPath("$[*].paymentPolicy").asArray()
            .containsOnly("ON_SITE");
        assertThat(json).bodyJson().extractingPath("$[*].depositCents").asArray()
            .containsOnlyNulls();
    }

    @Test
    void theDescriptionsAreTheValidatedTextsAndAreNotTruncated() {
        var json = mvc.get().uri("/api/offerings");

        assertThat(json).bodyJson().extractingPath("$[0].description").isEqualTo(
            "Une heure d'échange pour poser vos questions et recevoir un éclairage sur votre situation. "
                + "La séance se déroule à votre rythme.");
        assertThat(json).bodyJson().extractingPath("$[1].description").isEqualTo(
            "Un format court, pour faire le point sur une question précise ou une période de votre vie.");
        assertThat(json).bodyJson().extractingPath("$[2].description").isEqualTo(
            "Séance de magnétisme, pratique de bien-être qui se déroule en douceur. "
                + "Durée de 30 à 45 minutes selon la séance. Elle ne remplace pas un suivi médical.");
        assertThat(json).bodyJson().extractingPath("$[3].description").isEqualTo(
            "Séance de bien-être pour se recentrer et retrouver du calme. "
                + "Durée de 45 minutes à 1 heure selon le besoin. "
                + "Trois séances espacées d'un mois ou plus sont conseillées, sans quantité imposée. "
                + "Elle ne remplace pas un suivi médical.");
    }
}
