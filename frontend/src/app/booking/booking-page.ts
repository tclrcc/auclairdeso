import { Component, computed, effect, inject, input, linkedSignal, signal, viewChild } from '@angular/core';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { form, FormField, FormRoot, maxLength, pattern, required, validate } from '@angular/forms/signals';
import { AuthSession } from '../core/auth/auth-session';
import { ConsultationMode, MODE_LABELS, Offering } from '../public/offerings/offering';
import { EurosPipe } from '../shared/euros-pipe';
import { FieldErrors } from '../shared/field-errors';
import { formatDayOf, formatTime, parisDate, todayInParis } from '../shared/paris-time';
import { problemMessage } from '../shared/problem-message';
import { AppointmentView, ClientProfile, MODE_HINTS } from './booking';
import { BookingApi } from './booking-api';
import { BookingFormModel, emptyBookingForm, fromProfile, isAdult, toBookingRequest } from './booking-form-model';
import { SlotPicker } from './slot-picker';

const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const PHONE_PATTERN = /^\+?[0-9][0-9 .-]{8,18}$/;

@Component({
  selector: 'app-booking-page',
  imports: [FormField, FormRoot, RouterLink, EurosPipe, FieldErrors, SlotPicker],
  templateUrl: './booking-page.html',
})
export class BookingPage {
  private readonly api = inject(BookingApi);
  protected readonly auth = inject(AuthSession);
  private readonly slotPicker = viewChild(SlotPicker);

  /** Bound from the route and its query parameters (?mode=...&debut=...). */
  readonly slug = input.required<string>();
  readonly mode = input<string>();
  readonly debut = input<string>();

  protected readonly modeLabels = MODE_LABELS;
  protected readonly modeHints = MODE_HINTS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected readonly offering = httpResource<Offering>(() => `/api/offerings/${encodeURIComponent(this.slug())}`);
  private readonly profile = httpResource<ClientProfile | null>(() =>
    this.auth.isAuthenticated() ? '/api/bookings/profile' : undefined,
  );

  private readonly trusted = computed(() => this.profile.hasValue() && this.profile.value()?.trusted === true);
  protected readonly availableModes = computed<ConsultationMode[]>(() =>
    this.offering.hasValue()
      ? this.offering.value().modes.filter((mode) => mode !== 'IN_PERSON' || this.trusted())
      : [],
  );
  protected readonly needsPhoto = computed(
    () => this.offering.hasValue() && this.offering.value().category === 'CLAIRVOYANCE',
  );

  /** Keeps the client's choice when the list of modes changes, if it is still offered. */
  protected readonly selectedMode = linkedSignal<ConsultationMode[], ConsultationMode | null>({
    source: this.availableModes,
    computation: (modes, previous) => {
      const kept = previous?.value;
      if (kept && modes.includes(kept)) {
        return kept;
      }
      return modes.find((mode) => mode === this.mode()) ?? (modes.length === 1 ? modes[0] : null);
    },
  });
  protected readonly selectedStart = linkedSignal<string | null>(() => this.debut() ?? null);
  protected readonly initialDay = computed(() => {
    const debut = this.debut();
    return debut && !Number.isNaN(Date.parse(debut)) ? parisDate(debut) : undefined;
  });

  /** This page with the current choices, to come back here after login. */
  protected readonly returnUrl = computed(() => {
    const params = new URLSearchParams();
    const mode = this.selectedMode();
    const start = this.selectedStart();
    if (mode) {
      params.set('mode', mode);
    }
    if (start) {
      params.set('debut', start);
    }
    const query = params.toString();
    return `/reserver/${this.slug()}${query ? `?${query}` : ''}`;
  });

  protected readonly photo = signal<File | null>(null);
  protected readonly photoPreview = signal<string | null>(null);
  protected readonly photoError = signal<string | null>(null);
  protected readonly sent = signal<AppointmentView | null>(null);

  /** Empty, then prefilled once the profile of a returning client arrives. */
  private readonly model = linkedSignal<BookingFormModel>(() => {
    const profile = this.profile.hasValue() ? this.profile.value() : null;
    return profile ? fromProfile(profile) : emptyBookingForm();
  });

  protected readonly bookingForm = form(
    this.model,
    (path) => {
      required(path.firstName, { message: 'Le prénom est obligatoire.' });
      maxLength(path.firstName, 80, { message: '80 caractères au maximum.' });
      required(path.lastName, { message: 'Le nom est obligatoire.' });
      maxLength(path.lastName, 80, { message: '80 caractères au maximum.' });
      required(path.birthDate, { message: 'La date de naissance est obligatoire.' });
      validate(path.birthDate, ({ value }) =>
        value() && !isAdult(value(), todayInParis())
          ? { kind: 'minor', message: 'Les séances sont réservées aux personnes majeures.' }
          : null,
      );
      required(path.phone, { message: 'Le téléphone est obligatoire.' });
      pattern(path.phone, PHONE_PATTERN, { message: 'Ce numéro de téléphone ne semble pas valide.' });
      maxLength(path.city, 100, { message: '100 caractères au maximum.' });
      required(path.reason, { message: 'Indiquez la raison de votre demande.' });
      maxLength(path.reason, 2000, { message: '2000 caractères au maximum.' });
      required(path.address, {
        when: () => this.selectedMode() === 'CLIENT_HOME',
        message: "Indiquez l'adresse de la séance.",
      });
      required(path.messengerName, {
        when: () => this.selectedMode() === 'VIDEO',
        message: 'Indiquez votre nom sur Messenger.',
      });
      validate(path.acceptedTerms, ({ value }) =>
        value() ? null : { kind: 'terms', message: 'Merci de confirmer ce point pour continuer.' },
      );
      validate(path.consentsToDataProcessing, ({ value }) =>
        value() ? null : { kind: 'consent', message: 'Votre accord est nécessaire pour préparer la séance.' },
      );
    },
    {
      submission: {
        action: async (field) => {
          const mode = this.selectedMode();
          const start = this.selectedStart();
          if (!mode || !start) {
            return { kind: 'incomplete', message: 'Choisissez un mode de consultation et un créneau.' };
          }
          if (this.needsPhoto() && !this.photo()) {
            return { kind: 'photo', message: 'Ajoutez votre photo portrait.' };
          }
          try {
            const appointment = await this.api.book(
              toBookingRequest(this.slug(), mode, start, field().value()),
              this.photo(),
            );
            this.sent.set(appointment);
            window.scrollTo({ top: 0 });
            return;
          } catch (error) {
            if (error instanceof HttpErrorResponse && error.status === 409) {
              this.selectedStart.set(null);
              this.slotPicker()?.reload();
            }
            return { kind: 'serverError', message: problemMessage(error) };
          }
        },
      },
    },
  );

  constructor() {
    // A temporary local URL to preview the chosen photo, released when it changes.
    effect((onCleanup) => {
      const file = this.photo();
      if (!file) {
        this.photoPreview.set(null);
        return;
      }
      const url = URL.createObjectURL(file);
      this.photoPreview.set(url);
      onCleanup(() => URL.revokeObjectURL(url));
    });
  }

  protected onPhotoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.photoError.set(null);
    if (file && !ACCEPTED_PHOTO_TYPES.includes(file.type)) {
      this.photoError.set('Choisissez une photo au format JPEG, PNG ou WebP.');
      this.photo.set(null);
      return;
    }
    if (file && file.size > MAX_PHOTO_BYTES) {
      this.photoError.set('La photo doit faire au plus 10 Mo.');
      this.photo.set(null);
      return;
    }
    this.photo.set(file);
  }
}
