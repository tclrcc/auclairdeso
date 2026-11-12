import { Component, afterNextRender, inject, input, signal } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthSession } from '../../core/auth/auth-session';
import { isSafeRedirect } from '../../core/auth/safe-redirect';

type VerifyState = 'idle' | 'verifying' | 'invalid' | 'error';

@Component({
  selector: 'app-verify-page',
  imports: [RouterLink],
  templateUrl: './verify-page.html',
})
export class VerifyPage {
  private readonly auth = inject(AuthSession);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);

  /** Bound from the ?redirect= query parameter: where to go once the address is confirmed. */
  readonly redirect = input<string>();

  /**
   * The token of the link, in its fragment (#token=...), which the browser never sends to a server:
   * it stays out of every access log. Read in the browser only, never while rendering on the server.
   */
  private readonly token = signal<string | null>(null);

  protected readonly state = signal<VerifyState>('idle');

  constructor() {
    afterNextRender(() => {
      const fragment = this.route.snapshot.fragment;
      this.token.set(new URLSearchParams(fragment ?? '').get('token'));
      if (fragment) {
        // Out of the address bar and the history: only the path and ?redirect= remain.
        this.location.replaceState(this.location.path(true).split('#')[0]);
      }
    });
  }

  protected async confirm(): Promise<void> {
    const token = this.token();
    if (!token) {
      this.state.set('invalid');
      return;
    }
    this.state.set('verifying');
    try {
      if (await this.auth.verify(token)) {
        const redirect = this.redirect();
        await this.router.navigateByUrl(
          isSafeRedirect(redirect) ? redirect : this.auth.isStaff() ? '/admin' : '/mon-espace',
        );
      } else {
        this.state.set('invalid');
      }
    } catch {
      this.state.set('error');
    }
  }
}
