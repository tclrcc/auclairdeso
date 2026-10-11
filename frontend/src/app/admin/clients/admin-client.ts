import { AppointmentStatus } from '../../booking/booking';
import { ConsultationMode } from '../../public/offerings/offering';
import { ClientSummary } from '../agenda/admin-appointment';

export type ClientFilter = 'ALL' | 'TRUSTED' | 'BLOCKED';

/** The backend never sends more clients than this at once (ClientDirectory.MAX_RESULTS). */
export const LIST_LIMIT = 200;

export const FILTER_LABELS: Record<ClientFilter, string> = {
  ALL: 'Toutes',
  TRUSTED: 'De confiance',
  BLOCKED: 'Bloquées',
};

/** Mirror of the backend ClientHistoryEntry record. */
export interface ClientHistoryEntry {
  readonly id: number;
  readonly start: string;
  readonly offeringName: string;
  readonly mode: ConsultationMode;
  readonly status: AppointmentStatus;
  readonly priceCents: number;
  readonly lateCancellation: boolean;
}

/** Mirror of the backend ClientNoteView record. */
export interface ClientNote {
  readonly id: number;
  readonly text: string;
  readonly createdAt: string;
}

/** Mirror of the backend ClientFile record. */
export interface ClientFile {
  readonly client: ClientSummary;
  readonly bookedOnline: boolean;
  /** Another client, blocked, has the same phone number: she cannot book online with it. */
  readonly phoneBlocked: boolean;
  readonly lateCancellations: number;
  readonly lateCancellationsBeforeBlock: number;
  readonly appointments: readonly ClientHistoryEntry[];
  readonly notes: readonly ClientNote[];
}

/** The details form: every field is text, as the inputs give it. */
export interface CorrectionModel {
  firstName: string;
  lastName: string;
  birthDate: string;
  phone: string;
  city: string;
  email: string;
}

/** Mirror of the backend ClientCorrection record. */
export interface ClientCorrection {
  readonly firstName: string;
  readonly lastName: string;
  readonly birthDate: string | null;
  readonly phone: string;
  readonly city: string | null;
  readonly email: string | null;
}

/**
 * The list URL. One character would match almost everyone: no request until a second one is typed.
 * An empty search lists every client of the filter.
 */
export function clientsUrl(query: string, filter: ClientFilter): string | undefined {
  const text = query.trim();
  if (text.length === 1) {
    return undefined;
  }
  return `/api/admin/clients?query=${encodeURIComponent(text)}&filter=${filter}`;
}

export function toCorrectionModel(client: ClientSummary): CorrectionModel {
  return {
    firstName: client.firstName,
    lastName: client.lastName,
    birthDate: client.birthDate ?? '',
    phone: client.phone,
    city: client.city ?? '',
    email: client.email ?? '',
  };
}

export function toCorrection(model: CorrectionModel): ClientCorrection {
  return {
    firstName: model.firstName.trim(),
    lastName: model.lastName.trim(),
    birthDate: model.birthDate || null,
    phone: model.phone.trim(),
    city: model.city.trim() || null,
    email: model.email.trim() || null,
  };
}

const dateTime = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'Europe/Paris',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** "12 octobre 2026 à 14:05", in Paris time: notes and history span several years. */
export function formatDateTime(instant: string): string {
  return dateTime.format(new Date(instant));
}

const birthDateLabel = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'UTC',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

/** "4 mai 1990", for a YYYY-MM-DD date. */
export function formatBirthDate(date: string): string {
  return birthDateLabel.format(new Date(`${date}T00:00:00Z`));
}
