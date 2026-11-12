package fr.auclairdeso.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

/** As deployed (deploy/compose.yaml): springdoc off, and its paths closed by the security rules too. */
@SpringBootTest(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApiDocsAccessTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void theApiDocsAreClosedWhenSpringdocIsOff() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
