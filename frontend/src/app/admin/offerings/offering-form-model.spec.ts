import { AdminOffering } from './admin-offering';
import { emptyOfferingForm, toDraft, toFormModel } from './offering-form-model';

describe('offering form model', () => {
  it('converts euros to cents without floating point drift', () => {
    const draft = toDraft({
      ...emptyOfferingForm(),
      priceEuros: 19.99,
      paymentPolicy: 'DEPOSIT_ONLINE',
      depositEuros: 0.1 + 0.2,
    });

    expect(draft.priceCents).toBe(1999);
    expect(draft.depositCents).toBe(30);
  });

  it('drops the deposit unless the policy needs it', () => {
    const draft = toDraft({ ...emptyOfferingForm(), depositEuros: 30 });

    expect(draft.depositCents).toBeNull();
  });

  it('round-trips an existing offering', () => {
    const offering: AdminOffering = {
      slug: 'guidance-1-h',
      name: 'Guidance — 1 h',
      description: 'Une séance complète.',
      durationMinutes: 60,
      priceCents: 8000,
      paymentPolicy: 'DEPOSIT_ONLINE',
      depositCents: 3000,
      displayOrder: 20,
      active: true,
      modes: ['VIDEO', 'PHONE'],
    };

    expect(toDraft(toFormModel(offering))).toEqual({
      name: 'Guidance — 1 h',
      description: 'Une séance complète.',
      durationMinutes: 60,
      priceCents: 8000,
      paymentPolicy: 'DEPOSIT_ONLINE',
      depositCents: 3000,
      displayOrder: 20,
      active: true,
      modes: ['VIDEO', 'PHONE'],
    });
  });
});
