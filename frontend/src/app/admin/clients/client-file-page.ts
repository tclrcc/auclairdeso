import { Component, computed, inject, input, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import {
  email,
  form,
  FormField,
  FormRoot,
  maxLength,
  pattern,
  readonly,
  required,
  validate,
} from '@angular/forms/signals';
import { STATUS_LABELS } from '../../booking/booking';
import { MODE_LABELS } from '../../public/offerings/offering';
import { EurosPipe } from '../../shared/euros-pipe';
import { FieldErrors } from '../../shared/field-errors';
import { todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import { ageOn } from '../agenda/admin-appointment';
import {
  ClientFile,
  ClientNote,
  CorrectionModel,
  formatBirthDate,
  formatDateTime,
  toCorrection,
  toCorrectionModel,
} from './admin-client';
import { ClientAction, ClientsApi } from './clients-api';

const PHONE_PATTERN = /^\+?[0-9][0-9 .-]{8,18}$/;

@Component({
  selector: 'app-client-file-page',
  imports: [RouterLink, FormField, FormRoot, FieldErrors, EurosPipe],
  templateUrl: './client-file-page.html',
})
export class ClientFilePage {
  private readonly api = inject(ClientsApi);

  /** Bound from the route. */
  readonly id = input.required<string>();

  protected readonly file = httpResource<ClientFile>(() => `/api/admin/clients/${this.id()}`);
  protected readonly fullName = computed(() =>
    this.file.hasValue() ? `${this.file.value().client.firstName} ${this.file.value().client.lastName}` : '',
  );
  protected readonly age = computed(() => {
    const birthDate = this.file.hasValue() ? this.file.value().client.birthDate : null;
    return birthDate ? ageOn(birthDate, todayInParis()) : null;
  });
  protected readonly completedSessions = computed(() => {
    const count = this.file.hasValue()
      ? this.file.value().appointments.filter((entry) => entry.status === 'COMPLETED').length
      : 0;
    if (count === 0) {
      return 'Aucune séance effectuée.';
    }
    return count === 1 ? '1 séance effectuée.' : `${count} séances effectuées.`;
  });

  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDateTime = formatDateTime;
  protected readonly formatBirthDate = formatBirthDate;

  private readonly noteModel = signal({ text: '' });
  protected readonly noteForm = form(
    this.noteModel,
    (path) => {
      required(path.text, { message: 'La note est vide.' });
      maxLength(path.text, 4000, { message: '4000 caractères au maximum.' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            await this.api.addNote(Number(this.id()), field().value().text.trim());
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
          this.noteModel.set({ text: '' });
          this.noteForm().reset();
          this.file.reload();
          return;
        },
      },
    },
  );

  /** The details form opens on demand, filled with what the file says at that moment. */
  protected readonly editing = signal(false);
  private readonly correctionModel = signal<CorrectionModel>({
    firstName: '',
    lastName: '',
    birthDate: '',
    phone: '',
    city: '',
    email: '',
  });
  protected readonly correctionForm = form(
    this.correctionModel,
    (path) => {
      required(path.firstName, { message: 'Le prénom est obligatoire.' });
      maxLength(path.firstName, 80, { message: '80 caractères au maximum.' });
      required(path.lastName, { message: 'Le nom est obligatoire.' });
      maxLength(path.lastName, 80, { message: '80 caractères au maximum.' });
      required(path.phone, { message: 'Le téléphone est obligatoire.' });
      pattern(path.phone, PHONE_PATTERN, { message: 'Ce numéro de téléphone ne semble pas valide.' });
      validate(path.birthDate, ({ value }) =>
        value() && value() >= todayInParis()
          ? { kind: 'past', message: 'La date de naissance doit être dans le passé.' }
          : null,
      );
      maxLength(path.city, 100, { message: '100 caractères au maximum.' });
      email(path.email, { message: 'Cette adresse email ne semble pas valide.' });
      readonly(path.email, { when: () => this.file.hasValue() && this.file.value().bookedOnline });
    },
    {
      submission: {
        action: async (field) => {
          try {
            this.file.set(await this.api.correct(Number(this.id()), toCorrection(field().value())));
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
          this.editing.set(false);
          return;
        },
      },
    },
  );

  protected startEditing(): void {
    if (!this.file.hasValue()) {
      return;
    }
    this.correctionModel.set(toCorrectionModel(this.file.value().client));
    this.correctionForm().reset();
    this.editing.set(true);
  }

  protected block(): Promise<void> {
    return this.act('block',
      `Bloquer ${this.fullName()} ? Elle ne pourra plus réserver en ligne, ni avec son email ni avec son numéro.`);
  }

  protected unblock(): Promise<void> {
    return this.act('unblock', `Débloquer ${this.fullName()} ? Elle pourra de nouveau réserver en ligne, `
      + 'et ses annulations tardives passées ne compteront plus.');
  }

  protected async act(action: ClientAction, question?: string): Promise<void> {
    if (question && !confirm(question)) {
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    try {
      this.file.set(await this.api.apply(Number(this.id()), action));
    } catch (error) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected async deleteNote(note: ClientNote): Promise<void> {
    if (!confirm('Supprimer cette note ? Elle ne pourra pas être récupérée.')) {
      return;
    }
    this.actionError.set(null);
    try {
      await this.api.deleteNote(Number(this.id()), note.id);
      this.file.reload();
    } catch (error) {
      this.actionError.set(problemMessage(error));
    }
  }
}
