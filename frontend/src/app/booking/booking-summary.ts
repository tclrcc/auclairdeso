import { Component, input, output } from '@angular/core';
import { ConsultationMode, MODE_LABELS } from '../public/offerings/offering';
import { formatDuration } from '../shared/duration';
import { EurosPipe } from '../shared/euros-pipe';
import { formatDayOf, formatTime } from '../shared/paris-time';

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
  /** Whether the choices can still be changed, before sending. */
  readonly editable = input(false);
  readonly edit = output<BookingStep>();

  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDuration = formatDuration;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;
}
