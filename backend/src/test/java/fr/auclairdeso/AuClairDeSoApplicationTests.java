package fr.auclairdeso;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuClairDeSoApplicationTests {

    @Test
    void contextLoads() {
        // Starting the context proves that Flyway migrations apply on a real PostgreSQL
        // and that Hibernate's schema validation passes.
    }
}
