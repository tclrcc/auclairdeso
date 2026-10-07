import {ConsultationMode, OfferingCategory, PaymentPolicy} from '../../public/offerings/offering';

/** Mirror of the backend OfferingDetails record. Amounts are in euro cents. */
export interface AdminOffering {
  readonly slug: string;
  readonly name: string;
  readonly description: string;
  readonly durationMinutes: number;
  readonly bufferMinutes: number;
  readonly priceCents: number;
  readonly paymentPolicy: PaymentPolicy;
  readonly depositCents: number | null;
  readonly displayOrder: number;
  readonly active: boolean;
  readonly modes: readonly ConsultationMode[];
  readonly category: OfferingCategory;
}

/** Body sent to create or update an offering: mirror of the backend OfferingDraft record. */
export type OfferingDraft = Omit<AdminOffering, 'slug'>;

export const POLICY_LABELS: Record<PaymentPolicy, string> = {
  FULL_ONLINE: 'Paiement complet en ligne',
  DEPOSIT_ONLINE: 'Acompte en ligne, solde le jour de la séance',
  ON_SITE: 'Réglé après la séance, par virement ou en espèces',
};
