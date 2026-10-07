import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'seances', renderMode: RenderMode.Server },
  { path: 'connexion', renderMode: RenderMode.Client },
  { path: 'connexion/verifier', renderMode: RenderMode.Client },
  { path: 'mon-espace', renderMode: RenderMode.Client },
  { path: 'admin', renderMode: RenderMode.Client },
  { path: 'admin/verification', renderMode: RenderMode.Client },
  { path: 'admin/seances', renderMode: RenderMode.Client },
  { path: 'admin/seances/nouvelle', renderMode: RenderMode.Client },
  { path: 'admin/seances/modifier/:slug', renderMode: RenderMode.Client },
  { path: 'admin/agenda', renderMode: RenderMode.Client },
  { path: 'admin/agenda/:id', renderMode: RenderMode.Client },
  { path: 'reserver/:slug', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Prerender },
];
