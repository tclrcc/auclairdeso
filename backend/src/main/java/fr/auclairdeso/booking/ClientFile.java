package fr.auclairdeso.booking;

import java.util.List;

/**
 * Everything the practitioner knows about a client (docs/specification.md, 8).
 *
 * @param bookedOnline whether the client has booked on the site: her email is then her login, never changed
 * @param phoneBlocked whether another client, blocked, has the same phone number: she cannot book online with it
 * @param lateCancellations late cancellations since the last unblocking
 * @param lateCancellationsBeforeBlock how many of them block the client
 * @param appointments the most recent first
 * @param notes the most recent first
 */
record ClientFile(ClientSummary client, boolean bookedOnline, boolean phoneBlocked, int lateCancellations,
                  int lateCancellationsBeforeBlock, List<ClientHistoryEntry> appointments,
                  List<ClientNoteView> notes) {
}
