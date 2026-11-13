import { Component, computed, input, output } from '@angular/core';
import { ConsultationMode, MODE_LABELS } from '../public/offerings/offering';
import { formatDuration } from '../shared/duration';
import { EurosPipe } from '../shared/euros-pipe';
import { formatDayIn, formatDayOf, formatTime, formatTimeIn, isParisZone } from '../shared/paris-time';

export type BookingStep = 'mode' | 'slot';

/**
 * "Votre demande": what the client is about to send, or has just sent. Before sending, each choice can
 * be changed: the button sends her back to its step (the page moves the focus there).
 */
@Component({
  selector: 'app-booking-summary',
  imports: [EurosPipe],
  templateUrl: './booking-summary.html',
})
export class BookingSummary {
  readonly offeringName = input.required<string>();
  readonly durationMinutes = input.required<number>();
  readonly priceCents = input.required<number>();
  readonly mode = input<ConsultationMode | null>(null);
  readonly start = input<string | null>(null);
  /** The level of its title in the page where it sits. */
  readonly level = input<2 | 3>(2);
  /**
   * The visitor's time zone (an IANA name read by the browser). Times are always those of Paris, where
   * the practitioner works; when the visitor lives elsewhere, her own time is added beside them.
   */
  readonly visitorZone = input<string | null>(null);
  /** Whether the choices can still be changed, before sending. */
  readonly editable = input(false);
  readonly edit = output<BookingStep>();

  /** "soit 08:00 chez vous", with the day too when it differs, or null in Paris time. */
  protected readonly visitorTime = computed(() => {
    const start = this.start();
    const zone = this.visitorZone();
    if (!start || !zone || isParisZone(zone)) {
      return null;
    }
    const day = formatDayIn(start, zone);
    const time = formatTimeIn(start, zone);
    if (day === formatDayOf(start)) {
      return time === formatTime(start) ? null : `soit ${time} chez vous`;
    }
    return `soit le ${day} à ${time} chez vous`;
  });

  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDuration = formatDuration;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;
}
