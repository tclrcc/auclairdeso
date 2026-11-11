import { AdminOffering } from './admin-offering';
import { VIDEO_PATH_PATTERN, emptyOfferingForm, toDraft, toFormModel } from './offering-form-model';

const ADMIN: AdminOffering = {
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
  modes: ['VIDEO'],
  category: 'CLAIRVOYANCE',
  videoPath: null,
  videoTranscript: null,
};

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
      category: 'CLAIRVOYANCE',
      videoPath: null,
      videoTranscript: null,
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
      category: 'CLAIRVOYANCE',
      videoPath: null,
      videoTranscript: null,
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
      category: 'CLAIRVOYANCE',
      videoPath: null,
      videoTranscript: null,
    });
  });

  it('sends no video when the fields are empty or blank', () => {
    const draft = toDraft({ ...emptyOfferingForm(), videoPath: '  ', videoTranscript: '\n' });

    expect(draft.videoPath).toBeNull();
    expect(draft.videoTranscript).toBeNull();
  });

  it('round-trips the video, trimmed', () => {
    const model = {
      ...emptyOfferingForm(),
      videoPath: ' guidance-30-min.mp4 ',
      videoTranscript: ' Bonjour, je suis So. ',
    };

    expect(toDraft(model)).toMatchObject({
      videoPath: 'guidance-30-min.mp4',
      videoTranscript: 'Bonjour, je suis So.',
    });
    expect(toFormModel({ ...ADMIN, videoPath: 'a.webm', videoTranscript: 'Texte' })).toMatchObject({
      videoPath: 'a.webm',
      videoTranscript: 'Texte',
    });
    expect(toFormModel(ADMIN)).toMatchObject({ videoPath: '', videoTranscript: '' });
  });

  it('accepts a plain file name only', () => {
    for (const valid of ['guidance-1-h.mp4', 'a.webm', '1-seance.mp4']) {
      expect(VIDEO_PATH_PATTERN.test(valid), valid).toBe(true);
    }
    for (const invalid of ['videos/a.mp4', '../a.mp4', 'a..mp4', 'A.mp4', 'a.mov', 'a', '-a.mp4', '/a.mp4']) {
      expect(VIDEO_PATH_PATTERN.test(invalid), invalid).toBe(false);
    }
  });
});
