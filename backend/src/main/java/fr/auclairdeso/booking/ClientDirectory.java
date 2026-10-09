package fr.auclairdeso.booking;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looking up clients from the admin area. */
@Service
@Transactional(readOnly = true)
class ClientDirectory {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_RESULTS = 20;

    private final ClientRepository clients;

    ClientDirectory(ClientRepository clients) {
        this.clients = clients;
    }

    List<ClientSummary> search(String query) {
        var text = query.strip();
        if (text.length() < MIN_LENGTH) {
            return List.of();
        }
        return clients.search(text, PageRequest.of(0, MAX_RESULTS)).stream()
            .map(Client::toSummary)
            .toList();
    }
}
