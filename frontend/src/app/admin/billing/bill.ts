export type PaymentMethod = 'TRANSFER' | 'CASH' | 'MOBILE';

export const PAYMENT_METHODS: readonly PaymentMethod[] = ['TRANSFER', 'CASH', 'MOBILE'];

export const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  TRANSFER: 'Virement',
  CASH: 'Espèces',
  MOBILE: 'Paiement mobile',
};

/** Mirror of the backend BillView record. */
export interface Bill {
  readonly id: number;
  readonly number: string;
  readonly appointmentId: number;
  readonly issuedOn: string;
  readonly practitionerName: string;
  readonly practitionerAddress: string;
  readonly siret: string;
  readonly clientName: string;
  readonly clientEmail: string | null;
  readonly serviceStart: string;
  readonly servicePlace: string;
  readonly description: string;
  readonly amountCents: number;
  readonly vatMention: string;
  readonly paymentMethod: PaymentMethod | null;
  readonly paidOn: string | null;
}

/** Mirror of the backend ReceiptEntry record. */
export interface ReceiptEntry {
  readonly billId: number;
  readonly appointmentId: number;
  readonly paidOn: string;
  readonly number: string;
  readonly clientName: string;
  readonly description: string;
  readonly method: PaymentMethod;
  readonly amountCents: number;
}

/** Mirror of the backend ReceiptBook record. */
export interface ReceiptBook {
  readonly year: number;
  readonly entries: readonly ReceiptEntry[];
  readonly totalCents: number;
  readonly totalsByMethod: Record<PaymentMethod, number>;
}

/** "Encaissée le 18 octobre par virement", "… en espèces". */
export const PAYMENT_PHRASES: Record<PaymentMethod, string> = {
  TRANSFER: 'par virement',
  CASH: 'en espèces',
  MOBILE: 'par paiement mobile',
};

export interface MonthTotal {
  /** 1 to 12. */
  readonly month: number;
  readonly label: string;
  readonly totalCents: number;
}

const monthName = new Intl.DateTimeFormat('fr-FR', { month: 'long', timeZone: 'UTC' });

/** What was received each month, for the months that received something, in calendar order. */
export function monthlyTotals(entries: readonly ReceiptEntry[]): MonthTotal[] {
  const byMonth = new Map<number, number>();
  for (const entry of entries) {
    const month = Number(entry.paidOn.slice(5, 7));
    byMonth.set(month, (byMonth.get(month) ?? 0) + entry.amountCents);
  }
  return [...byMonth.entries()]
    .sort(([first], [second]) => first - second)
    .map(([month, totalCents]) => ({
      month,
      label: monthName.format(new Date(Date.UTC(2000, month - 1, 1))),
      totalCents,
    }));
}

/** The receipt book of a year, as a CSV file the browser downloads with the session cookie. */
export function exportUrl(year: number): string {
  return `/api/admin/receipts/export?year=${year}`;
}

const exactEuros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

/** "80,00 €": a bill always shows the cents. */
export function formatExactEuros(cents: number): string {
  return exactEuros.format(cents / 100);
}

const longDate = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'UTC',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

/** "18 octobre 2026", for a YYYY-MM-DD date. */
export function formatLongDate(date: string): string {
  return longDate.format(new Date(`${date}T00:00:00Z`));
}

const sessionDate = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'Europe/Paris',
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** "vendredi 16 octobre 2026 à 10:00", in Paris time. */
export function formatSessionDate(instant: string): string {
  return sessionDate.format(new Date(instant));
}
