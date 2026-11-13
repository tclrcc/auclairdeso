import { Component, ElementRef, Injector, afterNextRender, computed, inject, input, linkedSignal, model } from '@angular/core';
import { addMonths, formatDay, formatMonth, monthOf } from '../shared/paris-time';
import { isCalendarKey, monthWeeks, moveFrom } from './calendar-month';

interface CalendarCell {
  readonly date: string;
  readonly number: number;
  readonly label: string;
  readonly available: boolean;
  readonly today: boolean;
}

const WEEKDAYS = [
  { short: 'lun.', long: 'lundi' },
  { short: 'mar.', long: 'mardi' },
  { short: 'mer.', long: 'mercredi' },
  { short: 'jeu.', long: 'jeudi' },
  { short: 'ven.', long: 'vendredi' },
  { short: 'sam.', long: 'samedi' },
  { short: 'dim.', long: 'dimanche' },
];

/**
 * One month of days, where the client picks the day of her session. It only draws and navigates: which
 * days have slots comes from the parent (SlotPicker), which asks the backend.
 *
 * Keyboard and screen readers follow the grid pattern of the WAI-ARIA Authoring Practices (date picker):
 * one Tab stop, the arrows move by a day or a week, Home and End go to the ends of the week, Page Up and
 * Page Down change month, Enter or Space picks the day. A day without any slot can be reached, so that
 * moving stays predictable, but it cannot be picked (aria-disabled).
 *
 * A closed day, a full day and a day off all look the same: "pas de créneau". The backend does not tell
 * them apart, and the site has no reason to show the practitioner's absences or a feeling of scarcity.
 */
@Component({
  selector: 'app-availability-calendar',
  templateUrl: './availability-calendar.html',
})
export class AvailabilityCalendar {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  /** The days that have at least one slot (YYYY-MM-DD). */
  readonly available = input.required<ReadonlySet<string>>();
  /** The first and last days the calendar can reach (YYYY-MM-DD). */
  readonly first = input.required<string>();
  readonly last = input.required<string>();
  /** Today in Paris, computed by the browser: never bookable, only marked. */
  readonly today = input.required<string>();
  /** The month shown (YYYY-MM) and the day picked, shared with the parent. */
  readonly month = model.required<string>();
  readonly selected = model<string | null>(null);

  protected readonly weekdays = WEEKDAYS;
  protected readonly monthLabel = computed(() => formatMonth(this.month()));
  protected readonly canGoBack = computed(() => this.month() > monthOf(this.first()));
  protected readonly canGoForward = computed(() => this.month() < monthOf(this.last()));
  protected readonly previousLabel = computed(() => `Mois précédent, ${formatMonth(addMonths(this.month(), -1))}`);
  protected readonly nextLabel = computed(() => `Mois suivant, ${formatMonth(addMonths(this.month(), 1))}`);

  /**
   * The only day of the grid in the Tab order (roving tabindex): the picked day when it is shown, else
   * the first day with slots of the month, else its first reachable day.
   */
  protected readonly focusDate = linkedSignal(() => {
    const month = this.month();
    const selected = this.selected();
    if (selected && monthOf(selected) === month) {
      return selected;
    }
    const days = monthWeeks(month).flat().filter((day): day is string => day !== null);
    const reachable = days.filter((day) => this.isReachable(day));
    return reachable.find((day) => this.available().has(day)) ?? reachable[0] ?? days[0];
  });

  protected readonly weeks = computed<(CalendarCell | null)[][]>(() => {
    const available = this.available();
    const today = this.today();
    return monthWeeks(this.month()).map((week) =>
      week.map((date) => {
        if (!date) {
          return null;
        }
        const isAvailable = available.has(date) && this.isReachable(date);
        const isToday = date === today;
        return {
          date,
          number: Number(date.slice(8, 10)),
          available: isAvailable,
          today: isToday,
          label: `${formatDay(date)}${isToday ? ", aujourd'hui" : ''}, ${isAvailable ? 'disponible' : 'pas de créneau'}`,
        };
      }),
    );
  });

  protected previousMonth(): void {
    this.month.update((month) => addMonths(month, -1));
  }

  protected nextMonth(): void {
    this.month.update((month) => addMonths(month, 1));
  }

  protected pick(cell: CalendarCell): void {
    this.focusDate.set(cell.date);
    if (cell.available) {
      this.selected.set(cell.date);
    }
  }

  protected onKeydown(event: KeyboardEvent, cell: CalendarCell): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.pick(cell);
      return;
    }
    if (!isCalendarKey(event.key)) {
      return;
    }
    event.preventDefault();
    // From the day that holds the focus, not from the cell of the event: a held arrow key repeats faster
    // than the page renders, and each press must go one step further.
    const target = moveFrom(this.focusDate(), event.key, this.first(), this.last());
    this.month.set(monthOf(target));
    this.focusDate.set(target);
    const drawn = this.cellOf(target);
    if (drawn) {
      drawn.focus();
    } else {
      // The day belongs to a month that is not drawn yet: focus it once it is.
      afterNextRender(() => this.cellOf(target)?.focus(), { injector: this.injector });
    }
  }

  private isReachable(date: string): boolean {
    return date >= this.first() && date <= this.last();
  }

  private cellOf(date: string): HTMLElement | null {
    return this.host.nativeElement.querySelector<HTMLElement>(`[data-date="${date}"]`);
  }
}
