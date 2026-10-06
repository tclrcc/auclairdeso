import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth-guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Au clair de So — bientôt en ligne',
    loadComponent: () =>
      import('./public/coming-soon/coming-soon').then((m) => m.ComingSoon),
  },
  {
    path: 'seances',
    title: 'Séances et tarifs — Au clair de So',
    loadComponent: () =>
      import('./public/offerings/offerings-page').then((m) => m.OfferingsPage),
  },
  {
    path: 'connexion',
    title: 'Connexion — Au clair de So',
    loadComponent: () =>
      import('./public/login/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'connexion/verifier',
    title: 'Connexion — Au clair de So',
    loadComponent: () =>
      import('./public/login/verify-page').then((m) => m.VerifyPage),
  },
  {
    path: 'mon-espace',
    title: 'Mon espace — Au clair de So',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./account/account-page').then((m) => m.AccountPage),
  },
  { path: '**', redirectTo: '' },
];
