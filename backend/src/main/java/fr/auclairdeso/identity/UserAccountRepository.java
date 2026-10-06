package fr.auclairdeso.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findByRoleInOrderByEmailAsc(Collection<Role> roles);
}
