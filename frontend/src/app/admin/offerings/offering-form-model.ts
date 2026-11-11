import {ConsultationMode, MODE_LABELS, OfferingCategory} from '../../public/offerings/offering';
import { AdminOffering, OfferingDraft } from './admin-offering';

/** What the editor manipulates: euros instead of cents, one checkbox per mode. */
export interface OfferingFormModel {
  name: string;
  description: string;
  durationMinutes: number;
  bufferMinutes: number;
  priceEuros: number;
  displayOrder: number;
  active: boolean;
  modes: Record<ConsultationMode, boolean>;
  category: OfferingCategory;
}

export const ALL_MODES = Object.keys(MODE_LABELS) as ConsultationMode[];

export function emptyOfferingForm(): OfferingFormModel {
  return {
    name: '',
    description: '',
    durationMinutes: 60,
    bufferMinutes: 15,
    priceEuros: 0,
    displayOrder: 0,
    active: false,
    modes: modesRecord([]),
    category: 'CLAIRVOYANCE'
  };
}

export function toFormModel(offering: AdminOffering): OfferingFormModel {
  return {
    name: offering.name,
    description: offering.description,
    durationMinutes: offering.durationMinutes,
    bufferMinutes: offering.bufferMinutes,
    priceEuros: offering.priceCents / 100,
    displayOrder: offering.displayOrder,
    active: offering.active,
    modes: modesRecord(offering.modes),
    category: offering.category
  };
}

/**
 * The site takes no online payment: whatever an offering carried before, saving it sets the only
 * policy that is true (paid after the session) and drops any deposit. The backend still requires
 * both fields, so they are sent rather than omitted.
 */
export function toDraft(model: OfferingFormModel): OfferingDraft {
  return {
    name: model.name.trim(),
    description: model.description.trim(),
    durationMinutes: model.durationMinutes,
    bufferMinutes: model.bufferMinutes,
    priceCents: toCents(model.priceEuros),
    paymentPolicy: 'ON_SITE',
    depositCents: null,
    displayOrder: model.displayOrder,
    active: model.active,
    modes: ALL_MODES.filter((mode) => model.modes[mode]),
    category: model.category
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
