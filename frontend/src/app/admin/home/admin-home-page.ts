import { Component, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthSession } from '../../core/auth/auth-session';
import { problemMessage } from '../../shared/problem-message';

interface StaffMember {
  readonly email: string;
  readonly role: 'PRACTITIONER' | 'ADMIN';
  readonly passwordSet: boolean;
}

@Component({
  selector: 'app-admin-home-page',
  imports: [RouterLink],
  templateUrl: './admin-home-page.html',
})
export class AdminHomePage {
  protected readonly auth = inject(AuthSession);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly staff = httpResource<StaffMember[]>(() => '/api/admin/staff');
  protected readonly resetError = signal<string | null>(null);
  protected readonly pending = httpResource<unknown[]>(() => '/api/admin/appointments/pending', {
    defaultValue: [],
  });

  protected async resetPassword(member: StaffMember): Promise<void> {
    const confirmed = confirm(
      `Effacer le mot de passe de ${member.email} ?\n\n` +
      'Ses sessions seront fermées, et un nouveau mot de passe lui sera demandé à sa prochaine connexion.',
    );
    if (!confirmed) {
      return;
    }
    this.resetError.set(null);
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/admin/staff/${encodeURIComponent(member.email)}/password`),
      );
    } catch (error) {
      this.resetError.set(problemMessage(error));
      return;
    }
    if (member.email === this.auth.currentUser()?.email) {
      // Resetting your own password also ends your own session.
      await this.auth.refresh();
      await this.router.navigateByUrl('/connexion');
    } else {
      this.staff.reload();
    }
  }
}
