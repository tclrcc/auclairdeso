import { ConsultationMode } from '../public/offerings/offering';
import { BookingRequest, ClientProfile } from './booking';

/** What the client fills in; the mode, the slot and the photo are chosen separately. */
export interface BookingFormModel {
  firstName: string;
  lastName: string;
  birthDate: string;
  phone: string;
  city: string;
  reason: string;
  address: string;
  messengerName: string;
  acceptedTerms: boolean;
  consentsToDataProcessing: boolean;
}

export function emptyBookingForm(): BookingFormModel {
  return {
    firstName: '',
    lastName: '',
    birthDate: '',
    phone: '',
    city: '',
    reason: '',
    address: '',
    messengerName: '',
    acceptedTerms: false,
    consentsToDataProcessing: false,
  };
}

/** Prefills a returning client's details. Consents are asked again at each booking. */
export function fromProfile(profile: ClientProfile): BookingFormModel {
  return {
    ...emptyBookingForm(),
    firstName: profile.firstName,
    lastName: profile.lastName,
    birthDate: profile.birthDate,
    phone: profile.phone,
    city: profile.city ?? '',
  };
}

/** Whether someone born on birthDate is at least 18 on today (both YYYY-MM-DD). */
export function isAdult(birthDate: string, today: string): boolean {
  const [year, month, day] = birthDate.split('-');
  return `${Number(year) + 18}-${month}-${day}` <= today;
}

export function toBookingRequest(
  offeringSlug: string,
  mode: ConsultationMode,
  start: string,
  model: BookingFormModel,
): BookingRequest {
  return {
    offeringSlug,
    mode,
    start,
    client: {
      firstName: model.firstName.trim(),
      lastName: model.lastName.trim(),
      birthDate: model.birthDate,
      phone: model.phone.trim(),
      city: model.city.trim() || null,
    },
    reason: model.reason.trim(),
    address: mode === 'CLIENT_HOME' ? model.address.trim() : null,
    messengerName: mode === 'VIDEO' ? model.messengerName.trim() : null,
    acceptedTerms: model.acceptedTerms,
    consentsToDataProcessing: model.consentsToDataProcessing,
  };
}
