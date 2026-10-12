import { Component, input } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Bill, formatExactEuros, formatLongDate, formatSessionDate, PAYMENT_PHRASES } from './bill';

/**
 * A bill as a sheet of paper: to print and hand over, or to show on the screen to a client
 * who has no email. Every mention of the arrêté 83-50/A is there.
 */
@Component({
  selector: 'app-bill-page',
  imports: [RouterLink, NgOptimizedImage],
  templateUrl: './bill-page.html',
})
export class BillPage {
  /** Bound from the route. */
  readonly id = input.required<string>();

  protected readonly bill = httpResource<Bill>(() => `/api/admin/bills/${this.id()}`);

  protected readonly formatExactEuros = formatExactEuros;
  protected readonly formatLongDate = formatLongDate;
  protected readonly formatSessionDate = formatSessionDate;
  protected readonly methodPhrases = PAYMENT_PHRASES;

  protected print(): void {
    window.print();
  }
}
