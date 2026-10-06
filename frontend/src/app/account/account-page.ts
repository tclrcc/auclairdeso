import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';

@Component({
  selector: 'app-account-page',
  imports: [RouterLink],
  templateUrl: './account-page.html',
})
export class AccountPage {
  protected readonly auth = inject(AuthSession);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/');
  }
}
