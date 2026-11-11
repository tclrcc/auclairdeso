import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <main class="flex min-h-[60vh] flex-1 items-center px-5 py-20 sm:px-6">
      <div class="mx-auto max-w-lg text-center">
        <p class="font-display text-7xl text-gold/70">404</p>
        <h1 class="mt-4 font-display text-5xl text-cream">Cette page n’existe pas</h1>
        <p class="mt-4 text-ink-soft">
          Le lien est peut-être ancien, ou l’adresse mal recopiée. Voici par où reprendre.
        </p>
        <div class="mt-8 flex flex-wrap justify-center gap-3">
          <a routerLink="/" class="btn-primary">Accueil</a>
          <a routerLink="/seances" class="btn-gold-outline">Séances et tarifs</a>
        </div>
      </div>
    </main>
  `,
})
export class NotFoundPage {}
