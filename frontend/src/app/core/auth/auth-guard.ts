import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession } from './auth-session';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthSession);
  const router = inject(Router);

  const user = auth.currentUser() ?? (await auth.refresh());
  return user ? true : router.createUrlTree(['/connexion']);
};
