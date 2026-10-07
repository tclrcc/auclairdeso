import { Component, computed, inject, input, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { STATUS_LABELS } from '../../booking/booking';
import { MODE_LABELS } from '../../public/offerings/offering';
import { EurosPipe } from '../../shared/euros-pipe';
import { formatDayOf, formatTime, todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import { AdminAppointment, ageOn } from './admin-appointment';
import { AgendaApi } from './agenda-api';

@Component({
  selector: 'app-appointment-detail-page',
  imports: [RouterLink, EurosPipe],
  templateUrl: './appointment-detail-page.html',
})
export class AppointmentDetailPage {
  private readonly api = inject(AgendaApi);

  /** Bound from the route. */
  readonly id = input.required<string>();

  protected readonly appointment = httpResource<AdminAppointment>(() => `/api/admin/appointments/${this.id()}`);
  protected readonly photoUrl = computed(() => `/api/admin/appointments/${this.id()}/photo`);
  protected readonly age = computed(() =>
    this.appointment.hasValue() ? ageOn(this.appointment.value().client.birthDate, todayInParis()) : null,
  );

  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected confirm(): Promise<void> {
    return this.act((id) => this.api.confirm(id));
  }

  protected decline(): Promise<void> {
    if (!confirm('Refuser cette demande ?')) {
      return Promise.resolve();
    }
    return this.act((id) => this.api.decline(id));
  }

  private async act(action: (id: number) => Promise<unknown>): Promise<void> {
    this.busy.set(true);
    this.actionError.set(null);
    try {
      await action(Number(this.id()));
      this.appointment.reload();
    } catch (error) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busy.set(false);
    }
  }
}
