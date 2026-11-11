package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.auclairdeso.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The site takes no online payment (docs/specification.md, 3.5): whatever policy and deposit are received,
 * the offering is stored as ON_SITE without a deposit.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OfferingAdministration.class})
@Testcontainers(disabledWithoutDocker = true)
class OfferingAdministrationTests {

    @Autowired
    private OfferingAdministration administration;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @EnumSource(PaymentPolicy.class)
    void createStoresTheOfferingOnSiteWithoutDeposit(PaymentPolicy received) {
        var created = administration.create(draft("Guidance en ligne", received, 3_000));
        entityManager.flush();

        assertThat(created.paymentPolicy()).isEqualTo(PaymentPolicy.ON_SITE);
        assertThat(created.depositCents()).isNull();
        assertThat(storedPolicy(created.slug())).isEqualTo("ON_SITE");
        assertThat(storedDeposit(created.slug())).isNull();
    }

    @ParameterizedTest
    @EnumSource(PaymentPolicy.class)
    void updateKeepsTheOfferingOnSiteWithoutDeposit(PaymentPolicy received) {
        var slug = administration.create(draft("Magnétisme", PaymentPolicy.ON_SITE, null)).slug();
        entityManager.flush();

        administration.update(slug, draft("Magnétisme", received, 2_000));
        entityManager.flush();

        assertThat(storedPolicy(slug)).isEqualTo("ON_SITE");
        assertThat(storedDeposit(slug)).isNull();
    }

    @Test
    void storesTheVideoAndItsTranscriptThenClearsThemWhenEmptied() {
        var created = administration.create(withVideo("Guidance filmée", "guidance-30-min.mp4", "Bonjour."));
        entityManager.flush();

        assertThat(created.videoPath()).isEqualTo("guidance-30-min.mp4");
        assertThat(created.videoTranscript()).isEqualTo("Bonjour.");
        assertThat(jdbc.queryForObject("SELECT video_path FROM offering WHERE slug = ?", String.class,
            created.slug())).isEqualTo("guidance-30-min.mp4");

        var updated = administration.update(created.slug(), withVideo("Guidance filmée", "", "")).orElseThrow();
        entityManager.flush();

        assertThat(updated.videoPath()).isNull();
        assertThat(jdbc.queryForObject("SELECT video_path FROM offering WHERE slug = ?", String.class,
            created.slug())).isNull();
        assertThat(jdbc.queryForObject("SELECT video_transcript FROM offering WHERE slug = ?", String.class,
            created.slug())).isNull();
    }

    @Test
    void anOfferingWithoutVideoStaysValid() {
        var created = administration.create(draft("Sans vidéo", PaymentPolicy.ON_SITE, null));
        entityManager.flush();

        assertThat(created.videoPath()).isNull();
        assertThat(created.videoTranscript()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../etc/passwd", "videos/guidance.mp4", "guidance.mov", "Guidance.mp4"})
    void theDatabaseRefusesAVideoPathThatIsNotAPlainFileName(String path) {
        var created = administration.create(draft("Guidance", PaymentPolicy.ON_SITE, null));
        entityManager.flush();

        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> jdbc.update(
            "UPDATE offering SET video_path = ? WHERE slug = ?", path, created.slug()));
    }

    private static OfferingDraft withVideo(String name, String videoPath, String videoTranscript) {
        return new OfferingDraft(name, "Description", 60, 15, 8_000, PaymentPolicy.ON_SITE, null, 0, true,
            Set.of(ConsultationMode.PHONE), OfferingCategory.CLAIRVOYANCE, videoPath, videoTranscript);
    }

    private String storedPolicy(String slug) {
        return jdbc.queryForObject("SELECT payment_policy FROM offering WHERE slug = ?", String.class, slug);
    }

    private Integer storedDeposit(String slug) {
        return jdbc.queryForObject("SELECT deposit_cents FROM offering WHERE slug = ?", Integer.class, slug);
    }

    private static OfferingDraft draft(String name, PaymentPolicy policy, Integer depositCents) {
        return new OfferingDraft(name, "Description", 60, 15, 8_000, policy, depositCents, 0, true,
            Set.of(ConsultationMode.PHONE), OfferingCategory.CLAIRVOYANCE, null, null);
    }
}
