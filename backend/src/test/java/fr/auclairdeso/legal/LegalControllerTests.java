package fr.auclairdeso.legal;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.WebSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(LegalController.class)
@EnableConfigurationProperties(LegalProperties.class)
@Import(WebSecurityTestConfiguration.class)
@TestPropertySource(properties = {
    "auclairdeso.legal.publisher-name=Sophie Exemple EI",
    "auclairdeso.legal.address=1 place de la Mairie, 01150 Chazey-sur-Ain",
    "auclairdeso.legal.siret=123 456 789 00012",
    "auclairdeso.legal.contact-email=contact@example.com",
    "auclairdeso.legal.host=OVH SAS, Roubaix"})
class LegalControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void anyoneReadsTheLegalMentionsWithoutLoggingIn() {
        var result = mvc.get().uri("/api/legal").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.publisherName").isEqualTo("Sophie Exemple EI");
        assertThat(result).bodyJson().extractingPath("$.siret").isEqualTo("123 456 789 00012");
        // No mediator has been chosen yet: the pages say so rather than invent one.
        assertThat(result).bodyJson().extractingPath("$.mediator").isNull();
    }
}
