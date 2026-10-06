package fr.auclairdeso.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface OfferingRepository extends JpaRepository<Offering, Long> {

    @EntityGraph(attributePaths = "modes")
    List<Offering> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "modes")
    Optional<Offering> findBySlugAndActiveTrue(String slug);
}
