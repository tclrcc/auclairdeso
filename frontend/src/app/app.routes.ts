import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Au clair de So - bientôt en ligne',
    loadComponent: () =>
      import('./public/coming-soon/coming-soon').then((m) => m.ComingSoon),
  },
  { path: '**', redirectTo: '' },
];
