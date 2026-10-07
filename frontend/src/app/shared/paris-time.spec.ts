import {addDays, groupByParisDay, parisDate} from './paris-time';

describe('paris time', () => {
  it('gives the Paris date of an instant, even near midnight UTC', () => {
    expect(parisDate('2026-10-11T23:30:00Z')).toBe('2026-10-12');
  });

  it('adds days across months', () => {
    expect(addDays('2026-10-30', 3)).toBe('2026-11-02');
  });

  it('groups items by their Paris day', () => {
    const groups = groupByParisDay(
      ['2026-10-12T12:00:00Z', '2026-10-12T14:00:00Z', '2026-10-12T22:30:00Z'],
      (instant) => instant,
    );

    expect(groups.map((group) => [group.date, group.items.length])).toEqual([
      ['2026-10-12', 2],
      ['2026-10-13', 1],
    ]);
  });
});
