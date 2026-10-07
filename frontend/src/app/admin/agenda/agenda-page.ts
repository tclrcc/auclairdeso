import { Component, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MODE_LABELS } from '../../public/offerings/offering';
import { addDays, formatDayOf, formatTime, groupByParisDay, todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import { AdminAppointment } from './admin-appointment';
import { AgendaApi } from './agenda-api';

const UPCOMING_DAYS = 14;

@Component({
  selector: 'app-agenda-page',
  imports: [RouterLink],
  templateUrl: './agenda-page.html',
})
export class AgendaPage {
  private readonly api = inject(AgendaApi);
  private readonly today = todayInParis();

  protected readonly pending = httpResource<AdminAppointment[]>(() => '/api/admin/appointments/pending', {
    defaultValue: [],
  });
  protected readonly upcoming = httpResource<AdminAppointment[]>(
    () => `/api/admin/appointments?from=${this.today}&to=${addDays(this.today, UPCOMING_DAYS - 1)}`,
    { defaultValue: [] },
  );

  protected readonly confirmedDays = computed(() =>
    this.upcoming.hasValue()
      ? groupByParisDay(
        this.upcoming.value().filter((appointment) => appointment.status === 'CONFIRMED'),
        (appointment) => appointment.start,
      )
      : [],
  );

  /** The appointment whose action is running, to disable its buttons. */
  protected readonly busy = signal<number | null>(null);
  protected readonly actionError = signal<string | null>(null);

  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected confirm(appointment: AdminAppointment): Promise<void> {
    return this.act(appointment, () => this.api.confirm(appointment.id));
  }

  protected decline(appointment: AdminAppointment): Promise<void> {
    if (!confirm(`Refuser la demande de ${appointment.client.firstName} ${appointment.client.lastName} ?`)) {
      return Promise.resolve();
    }
    return this.act(appointment, () => this.api.decline(appointment.id));
  }

  private async act(appointment: AdminAppointment, action: () => Promise<unknown>): Promise<void> {
    this.busy.set(appointment.id);
    this.actionError.set(null);
    try {
      await action();
      this.pending.reload();
      this.upcoming.reload();
    } catch (error) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busy.set(null);
    }
  }
}
