import { erasureSummary } from './account-erasure';

describe('erasureSummary', () => {
  it('says that nothing was kept when there was nothing', () => {
    expect(erasureSummary({ appointments: 0, photos: 0, phoneKept: false })).toContain('Aucune séance passée');
  });

  it('counts the sessions kept, and says why', () => {
    const summary = erasureSummary({ appointments: 3, photos: 2, phoneKept: false });

    expect(summary).toContain('3 séances passées');
    expect(summary).toContain('dix ans');
    expect(summary).not.toContain('téléphone');
  });

  it('does not claim the name is removed: the bills keep it for ten years', () => {
    const summary = erasureSummary({ appointments: 1, photos: 0, phoneKept: false });

    expect(summary).not.toContain('sans votre nom');
    expect(summary).toContain('avec sa note et votre nom');
    expect(summary).toContain('dix ans');
    expect(summary).toContain('raisons comptables');
  });

  it('says what is deleted: e-mail address, login data and photos', () => {
    const summary = erasureSummary({ appointments: 2, photos: 1, phoneKept: false });

    expect(summary).toContain('adresse e-mail');
    expect(summary).toContain('données de connexion');
    expect(summary).toContain('photos');
    expect(summary).toContain('avec leurs notes et votre nom');
  });

  it('warns a blocked client that her phone number stays', () => {
    expect(erasureSummary({ appointments: 1, photos: 0, phoneKept: true })).toContain('numéro de téléphone reste');
  });
});
