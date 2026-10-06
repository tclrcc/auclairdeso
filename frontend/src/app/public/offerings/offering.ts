export type ConsultationMode = 'IN_PERSON' | 'VIDEO' | 'PHONE' | 'WRITTEN';

export type PaymentPolicy = 'FULL_ONLINE' | 'DEPOSIT_ONLINE' | 'ON_SITE';

/** Mirror of the backend OfferingView record. Amounts are in euro cents. */
export interface Offering {
  readonly slug: string;
  readonly name: string;
  readonly description: string;
  readonly durationMinutes: number;
  readonly priceCents: number;
  readonly paymentPolicy: PaymentPolicy;
  readonly depositCents: number | null;
  readonly modes: readonly ConsultationMode[];
}

export const MODE_LABELS: Record<ConsultationMode, string> = {
  IN_PERSON: 'Au cabinet',
  VIDEO: 'En visio',
  PHONE: 'Par téléphone',
  WRITTEN: 'Par écrit',
};
