const ZONE = 'Europe/Paris';

const isoDate = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});
const dayLabel = new Intl.DateTimeFormat('fr-FR', {
  timeZone: ZONE,
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});
const timeLabel = new Intl.DateTimeFormat('fr-FR', { timeZone: ZONE, hour: '2-digit', minute: '2-digit' });
// Calendar dates carry no time zone: they are read at noon UTC, which is the same day everywhere in Europe.
const monthLabel = new Intl.DateTimeFormat('fr-FR', { timeZone: 'UTC', month: 'long', year: 'numeric' });

/** The Paris calendar date of an instant, as YYYY-MM-DD. */
export function parisDate(instant: string | Date): string {
  return isoDate.format(new Date(instant));
}

export function todayInParis(): string {
  return parisDate(new Date());
}

/** Adds days to a YYYY-MM-DD date, without any time zone involved. */
export function addDays(date: string, days: number): string {
  const result = new Date(`${date}T00:00:00Z`);
  result.setUTCDate(result.getUTCDate() + days);
  return result.toISOString().slice(0, 10);
}

/** The month of a YYYY-MM-DD date, as YYYY-MM. */
export function monthOf(date: string): string {
  return date.slice(0, 7);
}

/** Adds months to a YYYY-MM month. */
export function addMonths(month: string, months: number): string {
  const [year, monthNumber] = month.split('-').map(Number);
  const index = year * 12 + (monthNumber - 1) + months;
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`;
}

/** The day of the week of a YYYY-MM-DD date, from 1 (Monday) to 7 (Sunday), as in ISO 8601. */
export function isoWeekday(date: string): number {
  return new Date(`${date}T12:00:00Z`).getUTCDay() || 7;
}

/** "novembre 2026", for a YYYY-MM month. */
export function formatMonth(month: string): string {
  return monthLabel.format(new Date(`${month}-15T12:00:00Z`));
}

/** "lundi 12 octobre", for a YYYY-MM-DD date. */
export function formatDay(date: string): string {
  return dayLabel.format(new Date(`${date}T12:00:00Z`));
}

/** "lundi 12 octobre", for an instant. */
export function formatDayOf(instant: string): string {
  return dayLabel.format(new Date(instant));
}

/** "14:00", in Paris time. */
export function formatTime(instant: string): string {
  return timeLabel.format(new Date(instant));
}

export interface DayGroup<T> {
  readonly date: string;
  readonly label: string;
  readonly items: readonly T[];
}

/** Groups items by their Paris calendar day, keeping their order. */
export function groupByParisDay<T>(items: readonly T[], instantOf: (item: T) => string): DayGroup<T>[] {
  const byDay = new Map<string, T[]>();
  for (const item of items) {
    const day = parisDate(instantOf(item));
    byDay.set(day, [...(byDay.get(day) ?? []), item]);
  }
  return [...byDay.entries()].map(([date, dayItems]) => ({ date, label: formatDay(date), items: dayItems }));
}
