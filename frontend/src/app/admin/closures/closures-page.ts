import { Component, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { form, FormField, FormRoot, maxLength, required, validate } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { FieldErrors } from '../../shared/field-errors';
import { formatDay } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';

interface Closure {
  readonly id: number;
  readonly firstDay: string;
  readonly lastDay: string;
  readonly reason: string | null;
}

const EMPTY = { firstDay: '', lastDay: '', reason: '' };

@Component({
  selector: 'app-closures-page',
  imports: [FormField, FormRoot, RouterLink, FieldErrors],
  templateUrl: './closures-page.html',
})
export class ClosuresPage {
  private readonly http = inject(HttpClient);

  protected readonly closures = httpResource<Closure[]>(() => '/api/admin/closures', { defaultValue: [] });
  protected readonly listError = signal<string | null>(null);
  protected readonly formatDay = formatDay;

  private readonly model = signal({ ...EMPTY });
  protected readonly closureForm = form(
    this.model,
    (path) => {
      required(path.firstDay, { message: 'Le premier jour est obligatoire.' });
      validate(path.lastDay, ({ value, valueOf }) =>
        value() && value() < valueOf(path.firstDay)
          ? { kind: 'order', message: 'Le dernier jour ne peut pas précéder le premier.' }
          : null,
      );
      maxLength(path.reason, 200, { message: '200 caractères au maximum.' });
    },
    {
      submission: {
        action: async (field) => {
          const { firstDay, lastDay, reason } = field().value();
          try {
            await firstValueFrom(
              this.http.post('/api/admin/closures', {
                firstDay,
                lastDay: lastDay || firstDay,
                reason: reason.trim() || null,
              }),
            );
            this.model.set({ ...EMPTY });
            this.closureForm().reset();
            this.closures.reload();
            return;
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
        },
      },
    },
  );

  protected async remove(closure: Closure): Promise<void> {
    if (!confirm('Supprimer cette fermeture ? Les créneaux de ces jours redeviendront réservables.')) {
      return;
    }
    this.listError.set(null);
    try {
      await firstValueFrom(this.http.delete(`/api/admin/closures/${closure.id}`));
      this.closures.reload();
    } catch (error) {
      this.listError.set(problemMessage(error));
    }
  }
}
