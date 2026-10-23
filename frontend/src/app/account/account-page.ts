import { Component, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AppointmentView, STATUS_LABELS } from '../booking/booking';
import { BookingApi } from '../booking/booking-api';
import { canCancel, cancellationQuestion } from '../booking/booking-cancellation';
import { AuthSession } from '../core/auth/auth-session';
import { MODE_LABELS } from '../public/offerings/offering';
import { EurosPipe } from '../shared/euros-pipe';
import { formatDayOf, formatTime } from '../shared/paris-time';
import { problemMessage } from '../shared/problem-message';
import { ErasureReport, erasureSummary } from './account-erasure';

@Component({
  selector: 'app-account-page',
  imports: [RouterLink, EurosPipe],
  templateUrl: './account-page.html',
})
export class AccountPage {
  protected readonly auth = inject(AuthSession);
  private readonly api = inject(BookingApi);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly bookings = httpResource<AppointmentView[]>(() => '/api/bookings/mine', {
    defaultValue: [],
  });
  protected readonly notice = signal<string | null>(null);
  protected readonly erasing = signal(false);
  protected readonly erased = signal<string | null>(null);
  protected readonly erasureError = signal<string | null>(null);

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

  /**
   * The right to erasure: the account goes, and the client reads what was kept. The server closes her
   * session, so the next request is anonymous; the page stays on screen for her to read.
   */
  protected async erase(): Promise<void> {
    const sure = confirm(
      'Supprimer définitivement votre compte et vos informations ?\n\n'
      + 'Vos séances passées sont conservées sans votre nom, avec leurs notes, que la loi impose de '
      + 'garder dix ans. Cette action ne peut pas être annulée.',
    );
    if (!sure) {
      return;
    }
    this.erasing.set(true);
    this.erasureError.set(null);
    try {
      const report = await firstValueFrom(this.http.delete<ErasureReport>('/api/account'));
      this.erased.set(erasureSummary(report));
      await this.auth.refresh();
    } catch (error) {
      this.erasureError.set(problemMessage(error));
    } finally {
      this.erasing.set(false);
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/');
  }
}
