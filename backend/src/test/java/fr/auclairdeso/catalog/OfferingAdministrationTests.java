package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
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

    private String storedPolicy(String slug) {
        return jdbc.queryForObject("SELECT payment_policy FROM offering WHERE slug = ?", String.class, slug);
    }

    private Integer storedDeposit(String slug) {
        return jdbc.queryForObject("SELECT deposit_cents FROM offering WHERE slug = ?", Integer.class, slug);
    }

    private static OfferingDraft draft(String name, PaymentPolicy policy, Integer depositCents) {
        return new OfferingDraft(name, "Description", 60, 15, 8_000, policy, depositCents, 0, true,
            Set.of(ConsultationMode.PHONE), OfferingCategory.CLAIRVOYANCE);
    }
}
