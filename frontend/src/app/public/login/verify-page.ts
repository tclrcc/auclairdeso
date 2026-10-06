import { Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthSession } from '../../core/auth/auth-session';

type VerifyState = 'idle' | 'verifying' | 'invalid' | 'error';

@Component({
  selector: 'app-verify-page',
  imports: [RouterLink],
  templateUrl: './verify-page.html',
})
export class VerifyPage {
  private readonly auth = inject(AuthSession);
  private readonly router = inject(Router);

  /** Bound from the ?token= query parameter. */
  readonly token = input<string>();

  protected readonly state = signal<VerifyState>('idle');

  protected async confirm(): Promise<void> {
    const token = this.token();
    if (!token) {
      this.state.set('invalid');
      return;
    }
    this.state.set('verifying');
    try {
      if (await this.auth.verify(token)) {
        await this.router.navigateByUrl(this.auth.isStaff() ? '/admin' : '/mon-espace');
      } else {
        this.state.set('invalid');
      }
    } catch {
      this.state.set('error');
    }
  }
}
