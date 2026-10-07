package fr.auclairdeso.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);
}
