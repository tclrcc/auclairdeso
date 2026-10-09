import { Component, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { AppointmentView, STATUS_LABELS } from '../booking/booking';
import { BookingApi } from '../booking/booking-api';
import { canCancel, cancellationQuestion } from '../booking/booking-cancellation';
import { AuthSession } from '../core/auth/auth-session';
import { MODE_LABELS } from '../public/offerings/offering';
import { EurosPipe } from '../shared/euros-pipe';
import { formatDayOf, formatTime } from '../shared/paris-time';
import { problemMessage } from '../shared/problem-message';

@Component({
  selector: 'app-account-page',
  imports: [RouterLink, EurosPipe],
  templateUrl: './account-page.html',
})
export class AccountPage {
  protected readonly auth = inject(AuthSession);
  private readonly api = inject(BookingApi);
  private readonly router = inject(Router);

  protected readonly bookings = httpResource<AppointmentView[]>(() => '/api/bookings/mine', {
    defaultValue: [],
  });
  protected readonly notice = signal<string | null>(null);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected canCancel(booking: AppointmentView): boolean {
    return canCancel(booking, new Date());
  }

  protected async cancel(booking: AppointmentView): Promise<void> {
    if (!confirm(cancellationQuestion(booking, new Date()))) {
      return;
    }
    try {
      const cancelled = await this.api.cancel(booking.id);
      this.notice.set(
        cancelled.lateCancellation
          ? 'Rendez-vous annulé. Cette annulation a été comptée comme tardive.'
          : 'Rendez-vous annulé.',
      );
      this.bookings.reload();
    } catch (error) {
      this.notice.set(problemMessage(error));
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/');
  }
}
