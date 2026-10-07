import { addDays, parisDate } from './paris-time';

describe('paris time', () => {
  it('gives the Paris date of an instant, even near midnight UTC', () => {
    expect(parisDate('2026-10-11T23:30:00Z')).toBe('2026-10-12');
  });

  it('adds days across months', () => {
    expect(addDays('2026-10-30', 3)).toBe('2026-11-02');
  });
});
