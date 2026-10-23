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

  it('warns a blocked client that her phone number stays', () => {
    expect(erasureSummary({ appointments: 1, photos: 0, phoneKept: true })).toContain('numéro de téléphone reste');
  });
});
