import { addDays, addMonths, formatMonth, groupByParisDay, isoWeekday, monthOf, parisDate } from './paris-time';

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

  it('gives the month of a date', () => {
    expect(monthOf('2026-11-19')).toBe('2026-11');
  });

  it('adds months across years, both ways', () => {
    expect(addMonths('2026-11', 2)).toBe('2027-01');
    expect(addMonths('2027-01', -1)).toBe('2026-12');
    expect(addMonths('2026-03', 0)).toBe('2026-03');
  });

  it('numbers the days of the week from Monday to Sunday', () => {
    expect(isoWeekday('2026-11-16')).toBe(1); // a Monday
    expect(isoWeekday('2026-11-19')).toBe(4); // a Thursday
    expect(isoWeekday('2026-11-22')).toBe(7); // a Sunday
  });

  it('names a month in French', () => {
    expect(formatMonth('2026-11')).toBe('novembre 2026');
  });
});
