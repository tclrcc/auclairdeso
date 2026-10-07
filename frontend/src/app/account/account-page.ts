import { Component, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { AppointmentView, STATUS_LABELS } from '../booking/booking';
import { AuthSession } from '../core/auth/auth-session';
import { MODE_LABELS } from '../public/offerings/offering';
import { EurosPipe } from '../shared/euros-pipe';
import { formatDayOf, formatTime } from '../shared/paris-time';

@Component({
  selector: 'app-account-page',
  imports: [RouterLink, EurosPipe],
  templateUrl: './account-page.html',
})
export class AccountPage {
  protected readonly auth = inject(AuthSession);
  private readonly router = inject(Router);

  protected readonly bookings = httpResource<AppointmentView[]>(() => '/api/bookings/mine', {
    defaultValue: [],
  });
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly formatDayOf = formatDayOf;
  protected readonly formatTime = formatTime;

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/');
  }
}
