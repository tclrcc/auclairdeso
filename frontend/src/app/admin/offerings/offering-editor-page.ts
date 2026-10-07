import { Component, computed, inject, input, linkedSignal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, maxLength, required, validate } from '@angular/forms/signals';
import {CATEGORY_LABELS, MODE_LABELS, OfferingCategory, PaymentPolicy} from '../../public/offerings/offering';
import { FieldErrors } from '../../shared/field-errors';
import { problemMessage } from '../../shared/problem-message';
import { AdminOffering, POLICY_LABELS } from './admin-offering';
import { OfferingAdmin } from './offering-admin';
import { ALL_MODES, OfferingFormModel, emptyOfferingForm, toDraft, toFormModel } from './offering-form-model';

@Component({
  selector: 'app-offering-editor-page',
  imports: [FormField, FormRoot, RouterLink, FieldErrors],
  templateUrl: './offering-editor-page.html',
})
export class OfferingEditorPage {
  private readonly admin = inject(OfferingAdmin);
  private readonly router = inject(Router);

  /** Bound from the route; absent when creating a new offering. */
  readonly slug = input<string>();

  protected readonly isEditing = computed(() => this.slug() != null);
  protected readonly modes = ALL_MODES;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly policies = Object.keys(POLICY_LABELS) as PaymentPolicy[];
  protected readonly policyLabels = POLICY_LABELS;
  protected readonly categories = Object.keys(CATEGORY_LABELS) as OfferingCategory[];
  protected readonly categoryLabels = CATEGORY_LABELS;

  protected readonly existing = httpResource<AdminOffering>(() => {
    const slug = this.slug();
    return slug ? `/api/admin/offerings/${encodeURIComponent(slug)}` : undefined;
  });

  /** Empty for a new offering, then reset with the loaded values when editing. */
  private readonly model = linkedSignal<OfferingFormModel>(() =>
    this.existing.hasValue() ? toFormModel(this.existing.value()) : emptyOfferingForm(),
  );

  protected readonly offeringForm = form(
    this.model,
    (path) => {
      required(path.name, { message: 'Le nom est obligatoire.' });
      maxLength(path.name, 120, { message: '120 caractères au maximum.' });
      required(path.description, { message: 'La description est obligatoire.' });
      maxLength(path.description, 4000, { message: '4000 caractères au maximum.' });
      validate(path.durationMinutes, ({ value }) =>
        Number.isInteger(value()) && value() >= 15 && value() <= 240
          ? null
          : { kind: 'duration', message: 'Une durée entière entre 15 et 240 minutes.' },
      );
      validate(path.bufferMinutes, ({ value }) =>
        Number.isInteger(value()) && value() >= 0 && value() <= 120
          ? null
          : { kind: 'buffer', message: 'Une pause entière entre 0 et 120 minutes.' },
      );
      validate(path.priceEuros, ({ value }) =>
        isBetween(value(), 0, 10_000) ? null : { kind: 'price', message: 'Un prix entre 0 et 10 000 €.' },
      );
      validate(path.depositEuros, ({ value, valueOf }) => {
        if (valueOf(path.paymentPolicy) !== 'DEPOSIT_ONLINE') {
          return null;
        }
        return isBetween(value(), 0.01, valueOf(path.priceEuros))
          ? null
          : { kind: 'deposit', message: "L'acompte doit être compris entre 0,01 € et le prix de la séance." };
      });
      validate(path.modes, ({ value }) =>
        Object.values(value()).some(Boolean)
          ? null
          : { kind: 'noMode', message: 'Choisissez au moins un mode de consultation.' },
      );
    },
    {
      submission: {
        action: async (field) => {
          const draft = toDraft(field().value());
          const slug = this.slug();
          try {
            if (slug) {
              await this.admin.update(slug, draft);
            } else {
              await this.admin.create(draft);
            }
            await this.router.navigateByUrl('/admin/seances');
            return;
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
        },
      },
    },
  );
}

function isBetween(value: number | null, min: number, max: number): boolean {
  return value != null && Number.isFinite(value) && value >= min && value <= max;
}
