import { Routes } from '@angular/router';
import { authGuard, staffGuard } from './core/auth/auth-guard';

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
  {
    path: 'admin',
    title: 'Administration — Au clair de So',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./admin/home/admin-home-page').then((m) => m.AdminHomePage),
  },
  {
    path: 'admin/verification',
    title: 'Vérification — Au clair de So',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./admin/password-step/password-step-page').then((m) => m.PasswordStepPage),
  },
  {
    path: 'admin/seances',
    title: 'Séances — Administration',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./admin/offerings/admin-offerings-page').then((m) => m.AdminOfferingsPage),
  },
  {
    path: 'admin/seances/nouvelle',
    title: 'Nouvelle séance — Administration',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./admin/offerings/offering-editor-page').then((m) => m.OfferingEditorPage),
  },
  {
    path: 'admin/seances/modifier/:slug',
    title: 'Modifier une séance — Administration',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./admin/offerings/offering-editor-page').then((m) => m.OfferingEditorPage),
  },
  { path: '**', redirectTo: '' },
];
