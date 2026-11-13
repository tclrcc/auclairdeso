import { addMonthsToDate, daysInMonth, isCalendarKey, monthWeeks, moveFrom } from './calendar-month';

describe('calendar month', () => {
  it('knows the length of each month, leap years included', () => {
    expect(daysInMonth('2026-11')).toBe(30);
    expect(daysInMonth('2026-12')).toBe(31);
    expect(daysInMonth('2027-02')).toBe(28);
    expect(daysInMonth('2028-02')).toBe(29);
  });

  it('lays out a month in weeks from Monday to Sunday, with empty cells around it', () => {
    // November 2026 starts on a Sunday and ends on a Monday.
    const weeks = monthWeeks('2026-11');

    expect(weeks).toHaveLength(6);
    expect(weeks.every((week) => week.length === 7)).toBe(true);
    expect(weeks[0]).toEqual([null, null, null, null, null, null, '2026-11-01']);
    expect(weeks[1][0]).toBe('2026-11-02');
    expect(weeks[5]).toEqual(['2026-11-30', null, null, null, null, null, null]);
  });

  it('needs no empty cell for a month that starts on a Monday and fills its weeks', () => {
    // February 2027 starts on a Monday and has exactly four weeks.
    const weeks = monthWeeks('2027-02');

    expect(weeks).toHaveLength(4);
    expect(weeks.flat()).not.toContain(null);
  });

  it('keeps the day when moving by months, or takes the last one of a shorter month', () => {
    expect(addMonthsToDate('2026-11-19', 1)).toBe('2026-12-19');
    expect(addMonthsToDate('2027-01-31', 1)).toBe('2027-02-28');
    expect(addMonthsToDate('2026-12-31', -1)).toBe('2026-11-30');
  });

  const FIRST = '2026-11-14';
  const LAST = '2027-01-14';

  it('moves by a day with the side arrows and by a week with the up and down arrows', () => {
    expect(moveFrom('2026-11-19', 'ArrowRight', FIRST, LAST)).toBe('2026-11-20');
    expect(moveFrom('2026-11-19', 'ArrowLeft', FIRST, LAST)).toBe('2026-11-18');
    expect(moveFrom('2026-11-19', 'ArrowDown', FIRST, LAST)).toBe('2026-11-26');
    expect(moveFrom('2026-11-30', 'ArrowRight', FIRST, LAST)).toBe('2026-12-01');
  });

  it('goes to the Monday and the Sunday of the week with Home and End', () => {
    expect(moveFrom('2026-11-19', 'Home', FIRST, LAST)).toBe('2026-11-16');
    expect(moveFrom('2026-11-19', 'End', FIRST, LAST)).toBe('2026-11-22');
  });

  it('changes month with Page Up and Page Down', () => {
    expect(moveFrom('2026-11-19', 'PageDown', FIRST, LAST)).toBe('2026-12-19');
    expect(moveFrom('2026-12-19', 'PageUp', FIRST, LAST)).toBe('2026-11-19');
  });

  it('never leaves the days the calendar can show', () => {
    expect(moveFrom('2026-11-16', 'ArrowUp', FIRST, LAST)).toBe(FIRST);
    expect(moveFrom('2026-11-16', 'PageUp', FIRST, LAST)).toBe(FIRST);
    expect(moveFrom('2027-01-10', 'PageDown', FIRST, LAST)).toBe(LAST);
  });

  it('recognises only the keys of the grid', () => {
    expect(isCalendarKey('PageDown')).toBe(true);
    expect(isCalendarKey('Enter')).toBe(false);
    expect(isCalendarKey('Tab')).toBe(false);
  });
});
