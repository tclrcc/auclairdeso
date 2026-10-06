package fr.auclairdeso.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface OfferingRepository extends JpaRepository<Offering, Long> {

    @EntityGraph(attributePaths = "modes")
    List<Offering> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "modes")
    Optional<Offering> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = "modes")
    List<Offering> findAllByOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "modes")
    Optional<Offering> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
