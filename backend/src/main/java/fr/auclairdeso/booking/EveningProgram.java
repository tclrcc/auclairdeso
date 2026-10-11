package fr.auclairdeso.booking;

import java.time.LocalDate;
import java.util.List;

/**
 * The practitioner's program for the next day, prepared every evening (docs/specification.md, 7).
 *
 * @param sessions the confirmed sessions of that day, in order
 * @param pendingRequests every request still waiting for an answer, whatever its day
 */
public record EveningProgram(LocalDate day, List<AppointmentNotice> sessions,
                             List<AppointmentNotice> pendingRequests) {
}
