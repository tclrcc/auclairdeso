import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalIdentityStore } from './legal-identity';
import { LegalPage } from './legal-page';

@Component({
  selector: 'app-mentions-legales-page',
  imports: [LegalPage, RouterLink],
  templateUrl: './mentions-legales-page.html',
})
export class MentionsLegalesPage {
  protected readonly legal = inject(LegalIdentityStore).identity;
}
