import { addDays, addMonths, isoWeekday, monthOf } from '../shared/paris-time';

/*
 * The month grid of the booking calendar, as pure functions on YYYY-MM-DD dates: no time zone, no
 * "today", nothing that differs between the server and the browser. Weeks start on Monday, as in France.
 */

/** The number of days of a YYYY-MM month. */
export function daysInMonth(month: string): number {
  const [year, monthNumber] = month.split('-').map(Number);
  // Day 0 of the next month is the last day of this one.
  return new Date(Date.UTC(year, monthNumber, 0)).getUTCDate();
}

/**
 * The weeks of a month, from Monday to Sunday. The days of the neighbouring months are null: the grid
 * shows them as empty cells, so that each column stays one day of the week.
 */
export function monthWeeks(month: string): (string | null)[][] {
  const first = `${month}-01`;
  const leading = isoWeekday(first) - 1;
  const cells: (string | null)[] = [
    ...Array.from({ length: leading }, () => null),
    ...Array.from({ length: daysInMonth(month) }, (_, index) => addDays(first, index)),
  ];
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  return Array.from({ length: cells.length / 7 }, (_, week) => cells.slice(week * 7, week * 7 + 7));
}

/** The same day in another month, or its last day when it is shorter (31 January + 1 month: 28 February). */
export function addMonthsToDate(date: string, months: number): string {
  const month = addMonths(monthOf(date), months);
  const day = Math.min(Number(date.slice(8, 10)), daysInMonth(month));
  return `${month}-${String(day).padStart(2, '0')}`;
}

/** The keys of the grid pattern (WAI-ARIA Authoring Practices, date picker). */
export type CalendarKey = 'ArrowLeft' | 'ArrowRight' | 'ArrowUp' | 'ArrowDown' | 'Home' | 'End' | 'PageUp' | 'PageDown';

const CALENDAR_KEYS: readonly string[] = [
  'ArrowLeft',
  'ArrowRight',
  'ArrowUp',
  'ArrowDown',
  'Home',
  'End',
  'PageUp',
  'PageDown',
];

export function isCalendarKey(key: string): key is CalendarKey {
  return CALENDAR_KEYS.includes(key);
}

/**
 * Where the focus goes from a date when a key is pressed, kept between first and last (the days the
 * calendar can show). Home and End go to the Monday and the Sunday of the week.
 */
export function moveFrom(date: string, key: CalendarKey, first: string, last: string): string {
  const target = (() => {
    switch (key) {
      case 'ArrowLeft':
        return addDays(date, -1);
      case 'ArrowRight':
        return addDays(date, 1);
      case 'ArrowUp':
        return addDays(date, -7);
      case 'ArrowDown':
        return addDays(date, 7);
      case 'Home':
        return addDays(date, 1 - isoWeekday(date));
      case 'End':
        return addDays(date, 7 - isoWeekday(date));
      case 'PageUp':
        return addMonthsToDate(date, -1);
      case 'PageDown':
        return addMonthsToDate(date, 1);
    }
  })();
  return target < first ? first : target > last ? last : target;
}
