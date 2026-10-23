import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalIdentityStore } from './legal-identity';
import { LegalPage } from './legal-page';

@Component({
  selector: 'app-conditions-generales-page',
  imports: [LegalPage, RouterLink],
  templateUrl: './conditions-generales-page.html',
})
export class ConditionsGeneralesPage {
  protected readonly legal = inject(LegalIdentityStore).identity;
}
