import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'seances', renderMode: RenderMode.Server },
  { path: '**', renderMode: RenderMode.Prerender },
];
