import { Component, computed, inject, input, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { STATUS_LABELS } from '../../booking/booking';
import { MODE_LABELS } from '../../public/offerings/offering';
import { EurosPipe } from '../../shared/euros-pipe';
import { formatDayOf, formatTime, todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import { AdminAppointment, ageOn } from './admin-appointment';
import { AgendaAction, AgendaApi } from './agenda-api';

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
  protected readonly age = computed(() => {
    const birthDate = this.appointment.hasValue() ? this.appointment.value().client.birthDate : null;
    return birthDate ? ageOn(birthDate, todayInParis()) : null;
  });
  protected readonly hasStarted = computed(
    () => this.appointment.hasValue() && new Date(this.appointment.value().start).getTime() <= Date.now(),
  );

  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected async act(action: AgendaAction, question?: string): Promise<void> {
    if (question && !confirm(question)) {
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    try {
      await this.api.apply(Number(this.id()), action);
      this.appointment.reload();
    } catch (error) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busy.set(false);
    }
  }
}
