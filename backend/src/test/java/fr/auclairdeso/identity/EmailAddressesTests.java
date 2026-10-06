package fr.auclairdeso.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAddressesTests {

    @Test
    void normalisesCaseAndSurroundingSpaces() {
        assertThat(EmailAddresses.normalize("  Client@Example.COM ")).isEqualTo("client@example.com");
    }

    @Test
    void rejectsImplausibleAddresses() {
        assertThat(EmailAddresses.normalize("pas-un-email")).isNull();
        assertThat(EmailAddresses.normalize("a@b")).isNull();
        assertThat(EmailAddresses.normalize("deux@@arobases.fr")).isNull();
    }

    @Test
    void acceptsNull() {
        assertThat(EmailAddresses.normalize(null)).isNull();
    }
}
