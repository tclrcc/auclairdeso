import { Component, computed, inject, input, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { AuthSession } from '../../core/auth/auth-session';

@Component({
  selector: 'app-login-page',
  imports: [FormField, FormRoot],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthSession);

  /** Bound from the ?redirect= query parameter: where the link of the email leads back to. */
  readonly redirect = input<string>();

  /** Coming from a booking, the email confirms the request: no account vocabulary. */
  protected readonly fromBooking = computed(() => this.redirect()?.startsWith('/reserver/') ?? false);

  /** Set once the link has been requested, to show the confirmation message. */
  protected readonly sentTo = signal<string | null>(null);

  private readonly model = signal({ email: '' });

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
          } catch {
            return {
              kind: 'serverError',
              message: "L'envoi du lien a échoué. Merci de réessayer dans un instant.",
            };
          }
        },
      },
    },
  );
}
