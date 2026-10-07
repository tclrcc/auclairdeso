import {Component, inject, input, signal} from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { AuthSession } from '../../core/auth/auth-session';
import {problemMessage} from '../../shared/problem-message';

@Component({
  selector: 'app-login-page',
  imports: [FormField, FormRoot],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthSession);

  /** Set once the link has been requested, to show the confirmation message. */
  protected readonly sentTo = signal<string | null>(null);

  private readonly model = signal({ email: '' });

  /** Bound from the ?redirect= query parameter: where to come back after login. */
  readonly redirect = input<string>();

  protected readonly loginForm = form(
    this.model,
    (path) => {
      required(path.email, { message: 'Indiquez votre adresse email.' });
      email(path.email, { message: 'Cette adresse email ne semble pas valide.' });
    },
    {
      submission: {
        action: async (field) => {
          const address = field().value().email.trim();
          try {
            await this.auth.requestMagicLink(address, this.redirect());
            this.sentTo.set(address);
            return;
          } catch (error) {
            return {
              kind: 'serverError',
              message: problemMessage(error)
            };
          }
        },
      },
    },
  );
}
