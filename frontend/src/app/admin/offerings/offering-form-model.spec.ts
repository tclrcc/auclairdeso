import { AdminOffering } from './admin-offering';
import { emptyOfferingForm, toDraft, toFormModel } from './offering-form-model';

describe('offering form model', () => {
  it('converts euros to cents without floating point drift', () => {
    const draft = toDraft({ ...emptyOfferingForm(), priceEuros: 19.99 });

    expect(draft.priceCents).toBe(1999);
  });

  it('never sends an online payment policy or a deposit, whatever the offering carried', () => {
    const offering: AdminOffering = {
      slug: 'guidance-1-h',
      name: 'Guidance — 1 h',
      description: 'Une séance complète.',
      durationMinutes: 60,
      bufferMinutes: 15,
      priceCents: 8000,
      paymentPolicy: 'DEPOSIT_ONLINE',
      depositCents: 3000,
      displayOrder: 20,
      active: true,
      modes: ['VIDEO'],
      category: 'CLAIRVOYANCE'
    };

    const draft = toDraft(toFormModel(offering));

    expect(draft.paymentPolicy).toBe('ON_SITE');
    expect(draft.depositCents).toBeNull();
  });

  it('round-trips an existing offering', () => {
    const offering: AdminOffering = {
      slug: 'guidance-1-h',
      name: 'Guidance — 1 h',
      description: 'Une séance complète.',
      durationMinutes: 60,
      bufferMinutes: 15,
      priceCents: 8000,
      paymentPolicy: 'ON_SITE',
      depositCents: null,
      displayOrder: 20,
      active: true,
      modes: ['VIDEO', 'PHONE'],
      category: 'CLAIRVOYANCE'
    };

    expect(toDraft(toFormModel(offering))).toEqual({
      name: 'Guidance — 1 h',
      description: 'Une séance complète.',
      durationMinutes: 60,
      bufferMinutes: 15,
      priceCents: 8000,
      paymentPolicy: 'ON_SITE',
      depositCents: null,
      displayOrder: 20,
      active: true,
      modes: ['VIDEO', 'PHONE'],
      category: 'CLAIRVOYANCE'
    });
  });
});
