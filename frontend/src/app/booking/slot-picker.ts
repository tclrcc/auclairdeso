import { Component, computed, input, linkedSignal, model } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { addDays, formatDay, formatTime, parisDate, todayInParis } from '../shared/paris-time';
import { TimeSlot } from './booking';

const HORIZON_DAYS = 62;

interface DaySlots {
  readonly date: string;
  readonly label: string;
  readonly slots: readonly TimeSlot[];
}

@Component({
  selector: 'app-slot-picker',
  templateUrl: './slot-picker.html',
})
export class SlotPicker {
  readonly slug = input.required<string>();
  /** First day to show (YYYY-MM-DD); tomorrow by default. */
  readonly from = input<string>();
  /** The chosen slot start, shared with the parent through [(selected)]. */
  readonly selected = model<string | null>(null);

  private readonly firstBookableDay = addDays(todayInParis(), 1);
  private readonly lastBookableDay = addDays(todayInParis(), HORIZON_DAYS);

  protected readonly weekStart = linkedSignal(() => {
    const from = this.from();
    return from && from > this.firstBookableDay ? from : this.firstBookableDay;
  });
  protected readonly weekEnd = computed(() => addDays(this.weekStart(), 6));
  protected readonly weekLabel = computed(() => `Du ${formatDay(this.weekStart())} au ${formatDay(this.weekEnd())}`);
  protected readonly canGoBack = computed(() => this.weekStart() > this.firstBookableDay);
  protected readonly canGoForward = computed(() => addDays(this.weekStart(), 7) <= this.lastBookableDay);

  protected readonly slots = httpResource<TimeSlot[]>(
    () =>
      `/api/offerings/${encodeURIComponent(this.slug())}/slots?from=${this.weekStart()}&to=${this.weekEnd()}`,
    { defaultValue: [] },
  );

  protected readonly days = computed<DaySlots[]>(() => {
    if (!this.slots.hasValue()) {
      return [];
    }
    const byDay = new Map<string, TimeSlot[]>();
    for (const slot of this.slots.value()) {
      const day = parisDate(slot.start);
      byDay.set(day, [...(byDay.get(day) ?? []), slot]);
    }
    return [...byDay.entries()].map(([date, slots]) => ({ date, label: formatDay(date), slots }));
  });

  protected readonly formatTime = formatTime;

  protected isSelected(slot: TimeSlot): boolean {
    const selected = this.selected();
    return selected != null && new Date(selected).getTime() === new Date(slot.start).getTime();
  }

  protected previousWeek(): void {
    this.weekStart.update((start) => {
      const previous = addDays(start, -7);
      return previous < this.firstBookableDay ? this.firstBookableDay : previous;
    });
  }

  protected nextWeek(): void {
    this.weekStart.update((start) => addDays(start, 7));
  }

  /** Asks the backend again, e.g. after someone else took the chosen slot. */
  reload(): void {
    this.slots.reload();
  }
}
