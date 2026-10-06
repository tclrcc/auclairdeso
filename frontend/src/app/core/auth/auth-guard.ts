import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession } from './auth-session';
import { hasBothFactors, isStaff } from './current-user';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthSession);
  const router = inject(Router);

  const user = auth.currentUser() ?? (await auth.refresh());
  return user ? true : router.createUrlTree(['/connexion']);
};

export const staffGuard: CanActivateFn = async () => {
  const auth = inject(AuthSession);
  const router = inject(Router);

  const user = auth.currentUser() ?? (await auth.refresh());
  if (!user) {
    return router.createUrlTree(['/connexion']);
  }
  if (!isStaff(user)) {
    return router.createUrlTree(['/mon-espace']);
  }
  return hasBothFactors(user) ? true : router.createUrlTree(['/admin/verification']);
};
