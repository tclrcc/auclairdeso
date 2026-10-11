package fr.auclairdeso.booking;

import java.time.OffsetDateTime;

/** A private note, as the client file shows it. */
record ClientNoteView(Long id, String text, OffsetDateTime createdAt) {
}
