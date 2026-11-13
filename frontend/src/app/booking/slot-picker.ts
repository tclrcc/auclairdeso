import { Component, ElementRef, Injector, afterNextRender, computed, effect, inject, input, linkedSignal, model, signal, untracked } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { addDays, formatDay, formatTime, groupByParisDay, monthOf, parisDate, todayInParis } from '../shared/paris-time';
import { AvailabilityCalendar } from './availability-calendar';
import { TimeSlot } from './booking';

/**
 * The longest period the backend serves in one request (SlotController.MAX_DAYS). It is only what is
 * asked: the backend keeps to its own horizon (auclairdeso.booking.horizon), and the calendar reaches
 * no further than the last slot it returns. No length of horizon is written in the front end.
 */
const MAX_DAYS = 62;

/**
 * Where the client picks her slot: a month calendar, then the times of the chosen day.
 *
 * All the slots of the horizon come in one request: a few kilobytes, after which changing month or day
 * asks the backend nothing. The route is rendered in the browser only (app.routes.server.ts), so "today"
 * is the browser's clock read in Paris time, never the server's.
 */
@Component({
  selector: 'app-slot-picker',
  imports: [AvailabilityCalendar],
  templateUrl: './slot-picker.html',
})
export class SlotPicker {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  readonly slug = input.required<string>();
  /** A day to show first (YYYY-MM-DD), e.g. the one of a slot kept through the login. */
  readonly from = input<string>();
  /** The chosen slot start, shared with the parent through [(selected)]. */
  readonly selected = model<string | null>(null);

  protected readonly today = todayInParis();
  protected readonly firstDay = addDays(this.today, 1);
  private readonly lastAsked = addDays(this.firstDay, MAX_DAYS);

  protected readonly slots = httpResource<TimeSlot[]>(
    () => `/api/offerings/${encodeURIComponent(this.slug())}/slots?from=${this.firstDay}&to=${this.lastAsked}`,
  );

  private readonly slotsByDay = computed(() => {
    const slots = this.slots.hasValue() ? this.slots.value() : [];
    return new Map(groupByParisDay(slots, (slot) => slot.start).map((group) => [group.date, group.items]));
  });
  protected readonly availableDays = computed<ReadonlySet<string>>(() => new Set(this.slotsByDay().keys()));
  protected readonly firstAvailable = computed(() => [...this.availableDays()].sort()[0] ?? null);
  /** The calendar goes as far as the last slot: past it, every day would read "pas de créneau". */
  protected readonly lastDay = computed(() => {
    const days = [...this.availableDays()].sort();
    const last = days[days.length - 1];
    return last && last > this.firstDay ? last : this.firstDay;
  });

  /**
   * The day whose times are shown. It follows the chosen slot; when the parent clears it (after the slot
   * was taken by someone else), the day stays, so that the client sees what is left on it.
   */
  protected readonly selectedDay = linkedSignal<string | null, string | null>({
    source: this.selected,
    computation: (selected, previous) => (selected ? parisDate(selected) : (previous?.value ?? null)),
  });

  /** The month shown: the chosen day's, else the one of the first slot, until the client goes elsewhere. */
  protected readonly month = linkedSignal<{ day: string | null; first: string | null }, string>({
    source: () => ({ day: this.selectedDay(), first: this.firstAvailable() }),
    computation: (source, previous) => {
      if (source.day) {
        return monthOf(source.day);
      }
      if (previous && previous.source.first === source.first) {
        return previous.value;
      }
      return monthOf(source.first ?? this.validFrom() ?? this.firstDay);
    },
  });

  protected readonly daySlots = computed(() => {
    const day = this.selectedDay();
    return day ? (this.slotsByDay().get(day) ?? []) : [];
  });
  protected readonly dayLabel = computed(() => {
    const day = this.selectedDay();
    return day ? formatDay(day) : '';
  });

  /** Said once when a slot kept from earlier (?debut=) is no longer offered. */
  protected readonly notice = signal<string | null>(null);

  protected readonly formatDay = formatDay;
  protected readonly formatTime = formatTime;
  /** Five weeks of empty days while the slots load, so that the page does not jump when they arrive. */
  protected readonly skeleton = Array.from({ length: 35 });

  constructor() {
    effect(() => {
      const selected = this.selected();
      if (!selected || !this.slots.hasValue() || this.slots.isLoading()) {
        return;
      }
      if (!this.slots.value().some((slot) => sameInstant(slot.start, selected))) {
        // Settle the day of that slot first: it stays open, with what is left on it, once the slot is cleared.
        untracked(this.selectedDay);
        this.notice.set("Le créneau choisi n'est plus disponible. Choisissez-en un autre.");
        this.selected.set(null);
      }
    });
  }

  protected isSelected(slot: TimeSlot): boolean {
    const selected = this.selected();
    return selected != null && sameInstant(selected, slot.start);
  }

  protected pickDay(day: string | null): void {
    this.selectedDay.set(day);
    const selected = this.selected();
    if (selected && parisDate(selected) !== day) {
      this.selected.set(null);
    }
    this.showTimes();
  }

  protected pickSlot(slot: TimeSlot): void {
    this.notice.set(null);
    this.selected.set(slot.start);
  }

  protected goToFirstAvailable(): void {
    const first = this.firstAvailable();
    if (first) {
      this.pickDay(first);
    }
  }

  /** Asks the backend again, e.g. after someone else took the chosen slot. */
  reload(): void {
    this.slots.reload();
  }

  private validFrom(): string | undefined {
    const from = this.from();
    return from && /^\d{4}-\d{2}-\d{2}$/.test(from) ? from : undefined;
  }

  /**
   * On a phone, the times sit under the calendar: bring them into view when they are off screen. The
   * scroll is smooth only when the visitor accepts motion (scroll-behavior in styles.css).
   */
  private showTimes(): void {
    afterNextRender(
      () => {
        const times = this.host.nativeElement.querySelector('#slot-times');
        if (times && times.getBoundingClientRect().bottom > window.innerHeight) {
          times.scrollIntoView({ block: 'nearest' });
        }
      },
      { injector: this.injector },
    );
  }
}

function sameInstant(a: string, b: string): boolean {
  return new Date(a).getTime() === new Date(b).getTime();
}
