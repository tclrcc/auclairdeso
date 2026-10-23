package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import fr.auclairdeso.WebSecurityTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ClientAdminController.class)
@Import(WebSecurityTestConfiguration.class)
class ClientAdminControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ClientDirectory directory;

    @MockitoBean
    private ClientFiles files;

    @Test
    void aClientNeverOpensAClientFile() {
        assertThat(mvc.get().uri("/api/admin/clients/7").with(user("client@example.com").roles("CLIENT")))
            .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(files);
    }

    @Test
    void theListShowsOnlyTheBlockedClientsOnDemand() {
        given(directory.search("", ClientFilter.BLOCKED)).willReturn(List.of(new ClientSummary(7L,
            "dora@example.com", "Dora", "Martin", null, "06 12 34 56 78", null, false, true, false)));

        assertThat(mvc.get().uri("/api/admin/clients?filter=BLOCKED").with(practitioner()))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$[0].blocked").isEqualTo(true);
    }

    @Test
    void anEmptyNoteIsRefused() {
        assertThat(mvc.post().uri("/api/admin/clients/7/notes").with(practitioner()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"text": "   "}
                """))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.errors[*].field").asArray().contains("text");
        verifyNoInteractions(files);
    }
}
