import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalIdentityStore } from './legal-identity';
import { LegalPage } from './legal-page';

@Component({
  selector: 'app-confidentialite-page',
  imports: [LegalPage, RouterLink],
  templateUrl: './confidentialite-page.html',
})
export class ConfidentialitePage {
  protected readonly legal = inject(LegalIdentityStore).identity;
}
