import { Routes } from '@angular/router';

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
  { path: '**', redirectTo: '' },
];
