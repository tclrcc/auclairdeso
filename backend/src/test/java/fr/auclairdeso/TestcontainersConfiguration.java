package fr.auclairdeso;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Turns off the mail health check of the actuator, for every test that mocks the mail sender.
     *
     * <p>Spring Boot registers a {@code JavaMailSenderImpl}, and its health check looks the bean up
     * by that exact class. A test replaces it with a mock of the {@code JavaMailSender} interface,
     * which is not a {@code JavaMailSenderImpl}: the check then finds nothing to probe and the whole
     * context fails to start. A test has no SMTP server to check anyway.
     */
    public static final String NO_MAIL_HEALTH_CHECK = "management.health.mail.enabled=false";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
