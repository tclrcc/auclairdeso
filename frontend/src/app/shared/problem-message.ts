import { HttpErrorResponse } from '@angular/common/http';

const FALLBACK = 'Une erreur est survenue. Merci de réessayer dans un instant.';

/** Turns an API ProblemDetail (detail + optional field errors) into one readable sentence. */
export function problemMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse) || typeof error.error?.detail !== 'string') {
    return FALLBACK;
  }
  const fieldMessages: string[] = (error.error.errors ?? []).map(
    (fieldError: { message: string }) => fieldError.message,
  );
  return [error.error.detail, ...fieldMessages].join(' ');
}
