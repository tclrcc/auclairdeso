import { ConsultationMode, MODE_LABELS, PaymentPolicy } from '../../public/offerings/offering';
import { AdminOffering, OfferingDraft } from './admin-offering';

/** What the editor manipulates: euros instead of cents, one checkbox per mode. */
export interface OfferingFormModel {
  name: string;
  description: string;
  durationMinutes: number;
  bufferMinutes: number;
  priceEuros: number;
  paymentPolicy: PaymentPolicy;
  depositEuros: number;
  displayOrder: number;
  active: boolean;
  modes: Record<ConsultationMode, boolean>;
}

export const ALL_MODES = Object.keys(MODE_LABELS) as ConsultationMode[];

export function emptyOfferingForm(): OfferingFormModel {
  return {
    name: '',
    description: '',
    durationMinutes: 60,
    bufferMinutes: 15,
    priceEuros: 0,
    paymentPolicy: 'ON_SITE',
    depositEuros: 0,
    displayOrder: 0,
    active: false,
    modes: modesRecord([]),
  };
}

export function toFormModel(offering: AdminOffering): OfferingFormModel {
  return {
    name: offering.name,
    description: offering.description,
    durationMinutes: offering.durationMinutes,
    bufferMinutes: offering.bufferMinutes,
    priceEuros: offering.priceCents / 100,
    paymentPolicy: offering.paymentPolicy,
    depositEuros: (offering.depositCents ?? 0) / 100,
    displayOrder: offering.displayOrder,
    active: offering.active,
    modes: modesRecord(offering.modes),
  };
}

export function toDraft(model: OfferingFormModel): OfferingDraft {
  return {
    name: model.name.trim(),
    description: model.description.trim(),
    durationMinutes: model.durationMinutes,
    bufferMinutes: model.bufferMinutes,
    priceCents: toCents(model.priceEuros),
    paymentPolicy: model.paymentPolicy,
    depositCents: model.paymentPolicy === 'DEPOSIT_ONLINE' ? toCents(model.depositEuros) : null,
    displayOrder: model.displayOrder,
    active: model.active,
    modes: ALL_MODES.filter((mode) => model.modes[mode]),
  };
}

function modesRecord(selected: readonly ConsultationMode[]): Record<ConsultationMode, boolean> {
  return Object.fromEntries(ALL_MODES.map((mode) => [mode, selected.includes(mode)])) as Record<
    ConsultationMode,
    boolean
  >;
}

/** 19.99 € → 1999. Rounding absorbs binary floating point errors (19.99 * 100 = 1998.9999…). */
function toCents(euros: number): number {
  return Math.round(euros * 100);
}
