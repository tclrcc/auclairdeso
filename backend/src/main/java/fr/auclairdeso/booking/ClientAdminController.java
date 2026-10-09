package fr.auclairdeso.booking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ClientAdminController {

    private final ClientDirectory directory;

    ClientAdminController(ClientDirectory directory) {
        this.directory = directory;
    }

    @GetMapping("/admin/clients")
    List<ClientSummary> search(@RequestParam(defaultValue = "") String query) {
        return directory.search(query);
    }
}
