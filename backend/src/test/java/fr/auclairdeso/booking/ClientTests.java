package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClientTests {

    @Test
    void theSecondLateCancellationBlocksTheClient() {
        var client = client();

        assertThat(client.recordLateCancellation(2)).isFalse();
        assertThat(client.recordLateCancellation(2)).isTrue();
        assertThat(client.isBlocked()).isTrue();
    }

    @Test
    void unblockingGivesAFreshStart() {
        var client = client();
        client.recordLateCancellation(2);
        client.recordLateCancellation(2);

        client.unblock();

        assertThat(client.isBlocked()).isFalse();
        assertThat(client.lateCancellations()).isZero();
        assertThat(client.recordLateCancellation(2)).isFalse();
    }

    @Test
    void thePractitionerCorrectsTheDetailsButNotTheEmail() {
        var client = client();

        client.correct(new ClientCorrection(" Alicia ", "Martin", null, "07 00 00 00 01", " ", "autre@example.com"));

        var summary = client.toSummary();
        assertThat(summary.firstName()).isEqualTo("Alicia");
        assertThat(summary.birthDate()).isNull();
        assertThat(summary.city()).isNull();
        assertThat(summary.email()).isEqualTo("alice@example.com");
    }

    private static Client client() {
        var client = new Client("alice@example.com");
        client.update(new ClientDetails("Alice", "Martin", LocalDate.of(1990, 5, 4), "06 12 34 56 78", "Lyon"));
        return client;
    }
}
