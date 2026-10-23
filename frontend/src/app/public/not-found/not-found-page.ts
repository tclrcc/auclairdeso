import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <main class="flex min-h-[60vh] items-center bg-sand px-5 py-20 text-night sm:px-6">
      <div class="mx-auto max-w-lg text-center">
        <p class="font-display text-7xl text-night/25">404</p>
        <h1 class="mt-4 font-display text-4xl tracking-tight">Cette page n’existe pas</h1>
        <p class="mt-4 text-night/80">
          Le lien est peut-être ancien, ou l’adresse mal recopiée. Voici par où reprendre.
        </p>
        <div class="mt-8 flex flex-wrap justify-center gap-3">
          <a routerLink="/" class="btn-primary">Accueil</a>
          <a routerLink="/seances" class="rounded-full border border-night/30 px-5 py-2">Séances et tarifs</a>
        </div>
      </div>
    </main>
  `,
})
export class NotFoundPage {}
