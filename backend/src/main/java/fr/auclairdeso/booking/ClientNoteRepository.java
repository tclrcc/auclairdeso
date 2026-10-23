package fr.auclairdeso.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClientNoteRepository extends JpaRepository<ClientNote, Long> {

    /** The most recent first; by id for two notes of the same instant. */
    List<ClientNote> findByClientOrderByCreatedAtDescIdDesc(Client client);

    /** Every note about this client, when she erases her account (docs/specification.md, 9). */
    int deleteByClient(Client client);

    /** A note of this client only: a note id taken from another file is simply not found. */
    Optional<ClientNote> findByIdAndClientId(Long id, Long clientId);
}
