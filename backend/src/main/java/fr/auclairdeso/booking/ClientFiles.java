package fr.auclairdeso.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client files of the practitioner (docs/specification.md, 8): details, history, private notes,
 * trust, block and unblock.
 */
@Service
@Transactional
class ClientFiles {

    private final ClientRepository clients;
    private final ClientNoteRepository notes;
    private final AppointmentRepository appointments;
    private final BookingRules rules;
    private final Clock clock;

    ClientFiles(ClientRepository clients, ClientNoteRepository notes, AppointmentRepository appointments,
                BookingRules rules, Clock clock) {
        this.clients = clients;
        this.notes = notes;
        this.appointments = appointments;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    ClientFile file(long id) {
        return fileOf(find(id));
    }

    ClientFile correct(long id, ClientCorrection correction) {
        var client = find(id);
        changeEmail(client, correction.email());
        client.correct(correction);
        return fileOf(client);
    }

    ClientFile trust(long id, boolean trusted) {
        return change(id, client -> client.markTrusted(trusted));
    }

    ClientFile block(long id) {
        return change(id, Client::block);
    }

    ClientFile unblock(long id) {
        return change(id, Client::unblock);
    }

    ClientNoteView addNote(long id, NoteRequest note) {
        var client = find(id);
        return notes.save(new ClientNote(client, note.text(), Instant.now(clock))).toView(rules.zone());
    }

    void deleteNote(long id, long noteId) {
        var note = notes.findByIdAndClientId(noteId, id)
            .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Note introuvable."));
        notes.delete(note);
    }

    /**
     * The email of a client who booked online is her login: changing it would cut her off from her bookings.
     * The email of a client entered by the practitioner may be added, fixed or removed.
     */
    private void changeEmail(Client client, @Nullable String requested) {
        var email = requested == null || requested.isBlank() ? null : requested.strip().toLowerCase(Locale.ROOT);
        if (Objects.equals(email, client.email())) {
            return;
        }
        if (bookedOnline(client)) {
            throw refused(HttpStatus.BAD_REQUEST,
                "Cette cliente réserve en ligne : son adresse email est son identifiant et ne peut pas être modifiée.");
        }
        if (email != null && clients.findByEmail(email).isPresent()) {
            throw refused(HttpStatus.CONFLICT, "Une autre cliente utilise déjà cette adresse email.");
        }
        client.changeEmail(email);
    }

    private ClientFile change(long id, Consumer<Client> change) {
        var client = find(id);
        change.accept(client);
        return fileOf(client);
    }

    private ClientFile fileOf(Client client) {
        var history = appointments.findByClientOrderByStartsAtDesc(client).stream()
            .map(appointment -> appointment.toHistoryEntry(rules.zone()))
            .toList();
        var clientNotes = notes.findByClientOrderByCreatedAtDescIdDesc(client).stream()
            .map(note -> note.toView(rules.zone()))
            .toList();
        var phoneBlocked = clients.isPhoneOfAnotherBlockedClient(client.phone(), client.id());
        return new ClientFile(client.toSummary(), bookedOnline(client), phoneBlocked, client.lateCancellations(),
            rules.lateCancellationsBeforeBlock(), history, clientNotes);
    }

    private boolean bookedOnline(Client client) {
        return appointments.existsByClientAndSource(client, AppointmentSource.ONLINE);
    }

    private Client find(long id) {
        return clients.findById(id).orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Cliente introuvable."));
    }

    private static BookingRefusedException refused(HttpStatus status, String detail) {
        return new BookingRefusedException(status, detail);
    }
}
