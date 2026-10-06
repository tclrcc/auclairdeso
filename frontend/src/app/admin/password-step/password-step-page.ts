import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, minLength, required } from '@angular/forms/signals';
import { AuthSession } from '../../core/auth/auth-session';

const SERVER_ERROR = 'Une erreur est survenue. Merci de réessayer dans un instant.';

@Component({
  selector: 'app-password-step-page',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './password-step-page.html',
})
export class PasswordStepPage {
  protected readonly auth = inject(AuthSession);
  private readonly router = inject(Router);

  /** Staff member who already has a password: second factor. */
  private readonly loginModel = signal({ password: '' });
  protected readonly loginForm = form(
    this.loginModel,
    (path) => {
      required(path.password, { message: 'Saisissez votre mot de passe.' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            if (await this.auth.confirmPassword(field().value().password)) {
              await this.router.navigateByUrl('/admin');
              return;
            }
            return { kind: 'wrongPassword', message: 'Mot de passe incorrect.', fieldTree: field.password };
          } catch {
            return { kind: 'serverError', message: SERVER_ERROR };
          }
        },
      },
    },
  );

  /** First visit: the staff member chooses their password. */
  private readonly createModel = signal({ password: '', confirmation: '' });
  protected readonly createForm = form(
    this.createModel,
    (path) => {
      required(path.password, { message: 'Choisissez un mot de passe.' });
      minLength(path.password, 12, { message: 'Au moins 12 caractères.' });
      required(path.confirmation, { message: 'Confirmez votre mot de passe.' });
    },
    {
      submission: {
        action: async (field) => {
          const { password, confirmation } = field().value();
          if (password !== confirmation) {
            return {
              kind: 'mismatch',
              message: 'Les deux mots de passe ne correspondent pas.',
              fieldTree: field.confirmation,
            };
          }
          try {
            await this.auth.definePassword(password);
            await this.router.navigateByUrl('/admin');
            return;
          } catch (error) {
            const detail = error instanceof HttpErrorResponse ? error.error?.detail : undefined;
            return { kind: 'serverError', message: detail ?? SERVER_ERROR };
          }
        },
      },
    },
  );
}
