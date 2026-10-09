import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { email, form, FormField, FormRoot, maxLength, pattern, required, validate } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { ConsultationMode, MODE_LABELS } from '../../public/offerings/offering';
import { FieldErrors } from '../../shared/field-errors';
import { problemMessage } from '../../shared/problem-message';
import { AdminOffering } from '../offerings/admin-offering';
import { AdminAppointment, ClientSummary } from './admin-appointment';

const PHONE_PATTERN = /^\+?[0-9][0-9 .-]{8,18}$/;

interface NewAppointmentModel {
  offeringSlug: string;
  mode: string;
  date: string;
  time: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  city: string;
  note: string;
  address: string;
  messengerName: string;
}

@Component({
  selector: 'app-new-appointment-page',
  imports: [FormField, FormRoot, RouterLink, FieldErrors],
  templateUrl: './new-appointment-page.html',
})
export class NewAppointmentPage {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly modeLabels = MODE_LABELS;

  protected readonly offerings = httpResource<AdminOffering[]>(() => '/api/admin/offerings', { defaultValue: [] });
  protected readonly activeOfferings = computed(() =>
    this.offerings.hasValue() ? this.offerings.value().filter((offering) => offering.active) : [],
  );
  protected readonly currentMode = computed(() => this.model().mode);

  /** Client search: at least 2 characters, then the backend answers at each keystroke. */
  protected readonly search = signal('');
  protected readonly matches = httpResource<ClientSummary[]>(
    () => {
      const text = this.search().trim();
      return text.length >= 2 ? `/api/admin/clients?query=${encodeURIComponent(text)}` : undefined;
    },
    { defaultValue: [] },
  );
  protected readonly selectedClient = signal<ClientSummary | null>(null);
  protected readonly creatingClient = signal(false);

  private readonly model = signal<NewAppointmentModel>({
    offeringSlug: '',
    mode: '',
    date: '',
    time: '',
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    city: '',
    note: '',
    address: '',
    messengerName: '',
  });

  protected readonly availableModes = computed<readonly ConsultationMode[]>(
    () => this.activeOfferings().find((offering) => offering.slug === this.model().offeringSlug)?.modes ?? [],
  );

  protected readonly appointmentForm = form(
    this.model,
    (path) => {
      required(path.offeringSlug, { message: 'Choisissez une séance.' });
      required(path.mode, { message: 'Choisissez un mode de consultation.' });
      validate(path.mode, ({ value }) =>
        value() && !this.availableModes().includes(value() as ConsultationMode)
          ? { kind: 'mode', message: "Ce mode n'est pas proposé pour cette séance." }
          : null,
      );
      required(path.date, { message: 'La date est obligatoire.' });
      required(path.time, { message: "L'heure est obligatoire." });
      required(path.firstName, { when: () => this.creatingClient(), message: 'Le prénom est obligatoire.' });
      required(path.lastName, { when: () => this.creatingClient(), message: 'Le nom est obligatoire.' });
      required(path.phone, { when: () => this.creatingClient(), message: 'Le téléphone est obligatoire.' });
      pattern(path.phone, PHONE_PATTERN, { message: 'Ce numéro de téléphone ne semble pas valide.' });
      email(path.email, { message: 'Cette adresse email ne semble pas valide.' });
      required(path.address, {
        when: () => this.model().mode === 'CLIENT_HOME',
        message: "Indiquez l'adresse de la séance.",
      });
      required(path.messengerName, {
        when: () => this.model().mode === 'VIDEO',
        message: 'Indiquez le nom Messenger de la cliente.',
      });
      maxLength(path.note, 2000, { message: '2000 caractères au maximum.' });
    },
    {
      submission: {
        action: async (field) => {
          const value = field().value();
          const client = this.selectedClient();
          if (!client && !this.creatingClient()) {
            return { kind: 'client', message: 'Choisissez une cliente, ou créez-en une nouvelle.' };
          }
          const body = {
            offeringSlug: value.offeringSlug,
            mode: value.mode,
            start: `${value.date}T${value.time}`,
            clientId: this.creatingClient() ? null : client?.id,
            newClient: this.creatingClient()
              ? {
                firstName: value.firstName.trim(),
                lastName: value.lastName.trim(),
                phone: value.phone.trim(),
                email: value.email.trim() || null,
                city: value.city.trim() || null,
              }
              : null,
            note: value.note.trim() || null,
            address: value.mode === 'CLIENT_HOME' ? value.address.trim() : null,
            messengerName: value.mode === 'VIDEO' ? value.messengerName.trim() : null,
          };
          try {
            const created = await firstValueFrom(this.http.post<AdminAppointment>('/api/admin/appointments', body));
            await this.router.navigate(['/admin/agenda', created.id]);
            return;
          } catch (error) {
            return { kind: 'serverError', message: problemMessage(error) };
          }
        },
      },
    },
  );

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected choose(client: ClientSummary): void {
    this.selectedClient.set(client);
    this.creatingClient.set(false);
  }

  protected startNewClient(): void {
    this.selectedClient.set(null);
    this.creatingClient.set(true);
  }

  protected resetClient(): void {
    this.selectedClient.set(null);
    this.creatingClient.set(false);
  }
}
