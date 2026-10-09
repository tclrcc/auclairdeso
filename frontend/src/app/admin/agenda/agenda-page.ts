import { Component, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MODE_LABELS } from '../../public/offerings/offering';
import { addDays, formatDayOf, formatTime, groupByParisDay, todayInParis } from '../../shared/paris-time';
import { problemMessage } from '../../shared/problem-message';
import { AdminAppointment } from './admin-appointment';
import { AgendaAction, AgendaApi } from './agenda-api';

const UPCOMING_DAYS = 14;

@Component({
  selector: 'app-agenda-page',
  imports: [RouterLink],
  templateUrl: './agenda-page.html',
})
export class AgendaPage {
  private readonly api = inject(AgendaApi);
  private readonly today = todayInParis();

  protected readonly toClose = httpResource<AdminAppointment[]>(() => '/api/admin/appointments/to-close', {
    defaultValue: [],
  });
  protected readonly pending = httpResource<AdminAppointment[]>(() => '/api/admin/appointments/pending', {
    defaultValue: [],
  });
  protected readonly upcoming = httpResource<AdminAppointment[]>(
    () => `/api/admin/appointments?from=${this.today}&to=${addDays(this.today, UPCOMING_DAYS - 1)}`,
    { defaultValue: [] },
  );

  /** Confirmed sessions still to come: those already started are in "to close". */
  protected readonly confirmedDays = computed(() =>
    this.upcoming.hasValue()
      ? groupByParisDay(
        this.upcoming
          .value()
          .filter((appointment) => appointment.status === 'CONFIRMED' && !this.hasStarted(appointment)),
        (appointment) => appointment.start,
      )
      : [],
  );

  protected readonly busy = signal<number | null>(null);
  protected readonly actionError = signal<string | null>(null);

  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected async act(appointment: AdminAppointment, action: AgendaAction, question?: string): Promise<void> {
    if (question && !confirm(question)) {
      return;
    }
    this.busy.set(appointment.id);
    this.actionError.set(null);
    try {
      await this.api.apply(appointment.id, action);
      this.toClose.reload();
      this.pending.reload();
      this.upcoming.reload();
    } catch (error) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busy.set(null);
    }
  }

  protected fullName(appointment: AdminAppointment): string {
    return `${appointment.client.firstName} ${appointment.client.lastName}`;
  }

  private hasStarted(appointment: AdminAppointment): boolean {
    return new Date(appointment.start).getTime() <= Date.now();
  }
}
