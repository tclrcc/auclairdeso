import { Component, computed, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { formatDay, formatDayOf, todayInParis } from '../../shared/paris-time';
import {
  Bill,
  exportUrl,
  formatExactEuros,
  monthlyTotals,
  PAYMENT_LABELS,
  PAYMENT_METHODS,
  ReceiptBook,
} from './bill';

/** Bills waiting for their payment, then the receipt book of a year with its totals. */
@Component({
  selector: 'app-receipts-page',
  imports: [RouterLink],
  templateUrl: './receipts-page.html',
})
export class ReceiptsPage {
  private readonly currentYear = Number(todayInParis().slice(0, 4));

  protected readonly year = signal(this.currentYear);
  protected readonly isCurrentYear = computed(() => this.year() === this.currentYear);

  protected readonly unpaid = httpResource<Bill[]>(() => '/api/admin/bills/unpaid', { defaultValue: [] });
  protected readonly unpaidTotal = computed(() =>
    this.unpaid.value().reduce((total, bill) => total + bill.amountCents, 0),
  );

  protected readonly book = httpResource<ReceiptBook>(() => `/api/admin/receipts?year=${this.year()}`);
  protected readonly months = computed(() => (this.book.hasValue() ? monthlyTotals(this.book.value().entries) : []));
  protected readonly exportUrl = computed(() => exportUrl(this.year()));

  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabels = PAYMENT_LABELS;
  protected readonly formatExactEuros = formatExactEuros;
  protected readonly formatDay = formatDay;
  protected readonly formatDayOf = formatDayOf;

  protected previousYear(): void {
    this.year.update((year) => year - 1);
  }

  protected nextYear(): void {
    this.year.update((year) => Math.min(year + 1, this.currentYear));
  }
}
