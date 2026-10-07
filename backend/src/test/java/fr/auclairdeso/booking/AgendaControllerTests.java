package fr.auclairdeso.booking;

import static fr.auclairdeso.TestUsers.practitioner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import fr.auclairdeso.WebSecurityTestConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AgendaController.class)
@Import(WebSecurityTestConfiguration.class)
class AgendaControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private Agenda agenda;

    @Test
    void aClientCannotSeeTheAgenda() {
        assertThat(mvc.get().uri("/api/admin/appointments/pending").with(user("client@example.com").roles("CLIENT")))
            .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void thePhotoIsServedWithItsTypeAndNeverCached() {
        given(agenda.photo(7L)).willReturn(Optional.of(new AppointmentPhoto(7L, Photo.of(TestBookings.JPEG))));

        var result = mvc.get().uri("/api/admin/appointments/7/photo").with(practitioner()).exchange();

        assertThat(result).hasStatusOk().hasContentType(MediaType.IMAGE_JPEG);
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }
}
