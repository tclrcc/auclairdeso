package fr.auclairdeso.booking;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    List<Client> findByBlockedTrue();

    /**
     * Whether a blocked client, other than the given one, has this phone number, however it is written
     * (docs/specification.md, 6). A few blocked clients at most: they are compared in memory.
     */
    default boolean isPhoneOfAnotherBlockedClient(String phone, @Nullable Long clientId) {
        return findByBlockedTrue().stream()
            .filter(blocked -> !Objects.equals(blocked.id(), clientId))
            .anyMatch(blocked -> Phones.same(blocked.phone(), phone));
    }

    /**
     * Clients whose name, email or phone contains the text (all of them when it is empty),
     * case-insensitive, optionally only the trusted or only the blocked ones.
     */
    @Query("""
            select c from Client c
            where (:text = ''
                   or lower(c.firstName) like lower(concat('%', :text, '%'))
                   or lower(c.lastName) like lower(concat('%', :text, '%'))
                   or lower(c.email) like lower(concat('%', :text, '%'))
                   or c.phone like concat('%', :text, '%'))
              and (:onlyTrusted = false or c.trusted = true)
              and (:onlyBlocked = false or c.blocked = true)
            order by c.lastName, c.firstName
            """)
    List<Client> search(String text, boolean onlyTrusted, boolean onlyBlocked, Pageable page);
}
