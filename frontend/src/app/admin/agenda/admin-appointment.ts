import { AppointmentStatus } from '../../booking/booking';
import { ConsultationMode } from '../../public/offerings/offering';

/** Mirror of the backend ClientSummary record. */
export interface ClientSummary {
  readonly id: number;
  readonly email: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly birthDate: string;
  readonly phone: string;
  readonly city: string | null;
  readonly trusted: boolean;
  readonly blocked: boolean;
}

/** Mirror of the backend AdminAppointmentView record. */
export interface AdminAppointment {
  readonly id: number;
  readonly offeringSlug: string;
  readonly offeringName: string;
  readonly mode: ConsultationMode;
  readonly start: string;
  readonly end: string;
  readonly status: AppointmentStatus;
  readonly priceCents: number;
  readonly reason: string;
  readonly address: string | null;
  readonly messengerName: string | null;
  readonly hasPhoto: boolean;
  readonly cancelledBy: 'CLIENT' | 'PRACTITIONER' | null;
  readonly lateCancellation: boolean;
  readonly client: ClientSummary;
}

/** Full years between a birth date and a day (both YYYY-MM-DD). */
export function ageOn(birthDate: string, day: string): number {
  const [birthYear, birthMonth, birthDay] = birthDate.split('-').map(Number);
  const [year, month, dayOfMonth] = day.split('-').map(Number);
  const birthdayPassed = month > birthMonth || (month === birthMonth && dayOfMonth >= birthDay);
  return year - birthYear - (birthdayPassed ? 0 : 1);
}
