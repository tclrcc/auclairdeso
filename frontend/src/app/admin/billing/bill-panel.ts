import { Component, computed, inject, input, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { form, FormField, FormRoot, required, validate } from '@angular/forms/signals';
import { FieldErrors } from '../../shared/field-errors';
import { todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import {
  Bill,
  formatExactEuros,
  formatLongDate,
  PAYMENT_LABELS,
  PAYMENT_METHODS,
  PAYMENT_PHRASES,
  PaymentMethod,
} from './bill';
import { BillingApi } from './billing-api';

/** The bill of a completed session, and its payment: shown in the appointment page. */
@Component({
  selector: 'app-bill-panel',
  imports: [RouterLink, FormField, FormRoot, FieldErrors],
  templateUrl: './bill-panel.html',
})
export class BillPanel {
  private readonly api = inject(BillingApi);

  readonly appointmentId = input.required<number>();

  /** A list: empty when the session has no bill (completed before billing existed, or free). */
  protected readonly bills = httpResource<Bill[]>(() => `/api/admin/bills?appointmentId=${this.appointmentId()}`, {
    defaultValue: [],
  });
  protected readonly bill = computed(() => this.bills.value()[0] ?? null);

  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabels = PAYMENT_LABELS;
  protected readonly methodPhrases = PAYMENT_PHRASES;
  protected readonly formatExactEuros = formatExactEuros;
  protected readonly formatLongDate = formatLongDate;
  protected readonly cancelError = signal<string | null>(null);

  private readonly model = signal({ method: '', paidOn: todayInParis() });
  protected readonly paymentForm = form(
    this.model,
    (path) => {
      required(path.method, { message: 'Choisissez le moyen de paiement.' });
      required(path.paidOn, { message: "La date d'encaissement est obligatoire." });
      validate(path.paidOn, ({ value }) =>
        value() && value() > todayInParis()
          ? { kind: 'future', message: "La date d'encaissement ne peut pas être dans le futur." }
          : null,
      );
    },
    {
      submission: {
        action: async (field) => {
          const bill = this.bill();
          if (!bill) {
            return;
          }
          const { method, paidOn } = field().value();
          try {
            this.bills.set([await this.api.pay(bill.id, method as PaymentMethod, paidOn)]);
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
          return;
        },
      },
    },
  );

  protected async cancelPayment(bill: Bill): Promise<void> {
    if (!confirm(`Annuler l'encaissement de la note ${bill.number} ? Il sortira du livre des recettes.`)) {
      return;
    }
    this.cancelError.set(null);
    try {
      this.bills.set([await this.api.cancelPayment(bill.id)]);
      this.model.set({ method: '', paidOn: todayInParis() });
      this.paymentForm().reset();
    } catch (error) {
      this.cancelError.set(problemMessage(error));
    }
  }
}
