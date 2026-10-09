import { ConsultationMode } from '../public/offerings/offering';

export interface TimeSlot {
  readonly start: string;
  readonly end: string;
}

export type AppointmentStatus =
  | 'REQUESTED'
  | 'CONFIRMED'
  | 'DECLINED'
  | 'EXPIRED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'NO_SHOW';

/** Mirror of the backend AppointmentView record. */
export interface AppointmentView {
  readonly id: number;
  readonly offeringSlug: string;
  readonly offeringName: string;
  readonly mode: ConsultationMode;
  readonly start: string;
  readonly end: string;
  readonly status: AppointmentStatus;
  readonly priceCents: number;
  readonly cancellationDeadline: string;
  readonly lateCancellation: boolean;
}

/** Mirror of the backend ClientProfile record. */
export interface ClientProfile {
  readonly firstName: string;
  readonly lastName: string;
  readonly birthDate: string | null;
  readonly phone: string;
  readonly city: string | null;
  readonly trusted: boolean;
}

export interface ClientDetails {
  readonly firstName: string;
  readonly lastName: string;
  readonly birthDate: string;
  readonly phone: string;
  readonly city: string | null;
}

/** Mirror of the backend BookingRequest record. */
export interface BookingRequest {
  readonly offeringSlug: string;
  readonly mode: ConsultationMode;
  readonly start: string;
  readonly client: ClientDetails;
  readonly reason: string;
  readonly address: string | null;
  readonly messengerName: string | null;
  readonly acceptedTerms: boolean;
  readonly consentsToDataProcessing: boolean;
}

export const STATUS_LABELS: Record<AppointmentStatus, string> = {
  REQUESTED: 'En attente de confirmation',
  CONFIRMED: 'Confirmé',
  DECLINED: 'Refusé',
  EXPIRED: 'Expiré',
  CANCELLED: 'Annulé',
  COMPLETED: 'Effectué',
  NO_SHOW: 'Absence',
};

export const MODE_HINTS: Record<ConsultationMode, string> = {
  IN_PERSON: "En présentiel. L'adresse vous sera envoyée avec la confirmation.",
  CLIENT_HOME: 'Chez vous, dans un rayon de 20 km autour de Chazey-sur-Ain.',
  VIDEO: 'Appel vidéo par Messenger.',
  PHONE: "Vous appelez à l'heure du rendez-vous ; le numéro vous sera envoyé avec la confirmation.",
};
