import { Routes } from '@angular/router';
import { authGuard, staffGuard } from './core/auth/auth-guard';
import { PublicLayout } from './layout/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Au clair de So — voyance, magnétisme et rééquilibrage énergétique dans l’Ain',
        loadComponent: () => import('./public/home/home-page').then((m) => m.HomePage),
      },
      {
        path: 'seances',
        title: 'Séances et tarifs — Au clair de So',
        loadComponent: () => import('./public/offerings/offerings-page').then((m) => m.OfferingsPage),
      },
      {
        path: 'reserver/:slug',
        title: 'Réserver une séance — Au clair de So',
        loadComponent: () => import('./booking/booking-page').then((m) => m.BookingPage),
      },
      {
        path: 'connexion',
        title: 'Confirmer mon adresse email — Au clair de So',
        loadComponent: () => import('./public/login/login-page').then((m) => m.LoginPage),
      },
      {
        path: 'connexion/verifier',
        title: 'Confirmer mon adresse email — Au clair de So',
        loadComponent: () => import('./public/login/verify-page').then((m) => m.VerifyPage),
      },
      {
        path: 'mon-espace',
        title: 'Mes rendez-vous — Au clair de So',
        canActivate: [authGuard],
        loadComponent: () => import('./account/account-page').then((m) => m.AccountPage),
      },
    ],
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
  {
    path: 'admin/agenda',
    title: 'Agenda — Administration',
    canActivate: [staffGuard],
    loadComponent: () => import('./admin/agenda/agenda-page').then((m) => m.AgendaPage),
  },
  {
    path: 'admin/agenda/nouveau',
    title: 'Ajouter un rendez-vous — Administration',
    canActivate: [staffGuard],
    loadComponent: () => import('./admin/agenda/new-appointment-page').then((m) => m.NewAppointmentPage),
  },
  {
    path: 'admin/fermetures',
    title: 'Fermetures — Administration',
    canActivate: [staffGuard],
    loadComponent: () => import('./admin/closures/closures-page').then((m) => m.ClosuresPage),
  },
  {
    path: 'admin/agenda/:id',
    title: 'Rendez-vous — Administration',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./admin/agenda/appointment-detail-page').then((m) => m.AppointmentDetailPage),
  },
  { path: '**', redirectTo: '' },
];
