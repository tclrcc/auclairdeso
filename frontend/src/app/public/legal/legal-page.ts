import { Component, input } from '@angular/core';
import { LEGAL_UPDATED_ON } from './legal-identity';

/** The shell the three legal pages share: a title, the date of the texts, then the text itself. */
@Component({
  selector: 'app-legal-page',
  template: `
    <main class="flex-1 pt-14 pb-section sm:pt-20">
      <div class="night-container">
        <header class="max-w-[70ch]">
          <h1 class="night-page-title">{{ heading() }}</h1>
          <p class="mt-4 text-sm text-ink-faint">Dernière mise à jour : {{ updatedOn }}</p>
        </header>
        <div class="legal-prose mt-12">
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
