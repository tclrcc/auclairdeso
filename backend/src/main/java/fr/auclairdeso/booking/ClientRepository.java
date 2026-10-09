package fr.auclairdeso.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    /** Clients whose name, email or phone contains the text, case-insensitive. */
    @Query("""
            select c from Client c
            where lower(c.firstName) like lower(concat('%', :text, '%'))
               or lower(c.lastName) like lower(concat('%', :text, '%'))
               or lower(c.email) like lower(concat('%', :text, '%'))
               or c.phone like concat('%', :text, '%')
            order by c.lastName, c.firstName
            """)
    List<Client> search(String text, Pageable page);
}
