package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.auclairdeso.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class OfferingRepositoryTests {

    @Autowired
    private OfferingRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsOnlyActiveOfferingsInDisplayOrderWithTheirModes() {
        var second = offering("guidance-1-h", 20, true, Set.of(ConsultationMode.PHONE, ConsultationMode.VIDEO));
        var first = offering("guidance-30-min", 10, true, Set.of(ConsultationMode.PHONE));
        var hidden = offering("ancienne-seance", 0, false, Set.of(ConsultationMode.IN_PERSON));
        repository.saveAll(List.of(second, first, hidden));
        entityManager.flush();
        entityManager.clear();

        var views = repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
            .map(Offering::toView)
            .toList();

        assertThat(views).extracting(OfferingView::slug)
            .containsExactly("guidance-30-min", "guidance-1-h");
        assertThat(views.get(1).modes())
            .containsExactly(ConsultationMode.VIDEO, ConsultationMode.PHONE);
    }

    @Test
    void databaseRejectsAnInvalidSlug() {
        var invalid = offering("Guidance 1h", 0, true, Set.of(ConsultationMode.PHONE));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
            .isThrownBy(() -> repository.saveAndFlush(invalid));
    }

    private static Offering offering(String slug, int displayOrder, boolean active, Set<ConsultationMode> modes) {
        return new Offering(slug, new OfferingDraft("Name " + slug, "Description", 60, 15, 8_000,
            PaymentPolicy.FULL_ONLINE, null, displayOrder, active, modes));
    }
}
