package fr.auclairdeso.booking;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looking up clients from the admin area: the client list, and the search of the manual appointment. */
@Service
@Transactional(readOnly = true)
class ClientDirectory {

    private static final int MAX_RESULTS = 200;

    private final ClientRepository clients;

    ClientDirectory(ClientRepository clients) {
        this.clients = clients;
    }

    /**
     * The clients whose name, email or phone contains the query; all of them when it is empty.
     * A single character would match almost everyone: it returns nothing.
     */
    List<ClientSummary> search(String query, ClientFilter filter) {
        var text = query.strip();
        if (text.length() == 1) {
            return List.of();
        }
        var page = PageRequest.of(0, MAX_RESULTS);
        return clients.search(text, filter == ClientFilter.TRUSTED, filter == ClientFilter.BLOCKED, page).stream()
            .map(Client::toSummary)
            .toList();
    }
}
