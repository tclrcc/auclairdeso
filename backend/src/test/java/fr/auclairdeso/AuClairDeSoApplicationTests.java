package fr.auclairdeso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuClairDeSoApplicationTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void contextLoads() {
        // Starting the context proves that Flyway migrations apply on a real PostgreSQL
        // and that Hibernate's schema validation passes.
    }

    @Test
    void theApiDocsAreOpenWhenSpringdocIsOn() {
        // springdoc's default, kept in development to read the API.
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }
}
