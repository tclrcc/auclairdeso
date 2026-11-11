import { Component, input } from '@angular/core';
import { LEGAL_UPDATED_ON } from './legal-identity';

/** The shell the three legal pages share: a title, the date of the texts, then the text itself. */
@Component({
  selector: 'app-legal-page',
  template: `
    <main class="flex-1 px-5 py-16 sm:px-6 sm:py-24">
      <div class="mx-auto max-w-2xl">
        <h1 class="font-display text-5xl text-cream sm:text-6xl">{{ heading() }}</h1>
        <p class="mt-3 text-sm text-ink-faint">Dernière mise à jour : {{ updatedOn }}</p>
        <div class="legal-prose mt-10">
          <ng-content />
        </div>
      </div>
    </main>
  `,
})
export class LegalPage {
  readonly heading = input.required<string>();
  protected readonly updatedOn = LEGAL_UPDATED_ON;
}
