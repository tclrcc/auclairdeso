export type ConsultationMode = 'IN_PERSON' | 'CLIENT_HOME' | 'VIDEO' | 'PHONE';

export type PaymentPolicy = 'FULL_ONLINE' | 'DEPOSIT_ONLINE' | 'ON_SITE';

export interface Offering {
  readonly slug: string;
  readonly name: string;
  readonly description: string;
  readonly durationMinutes: number;
  readonly bufferMinutes: number;
  readonly priceCents: number;
  readonly paymentPolicy: PaymentPolicy;
  readonly depositCents: number | null;
  readonly modes: readonly ConsultationMode[];
}

export const MODE_LABELS: Record<ConsultationMode, string> = {
  IN_PERSON: 'En présentiel',
  CLIENT_HOME: 'À domicile',
  VIDEO: 'En visio',
  PHONE: 'Par téléphone',
};
