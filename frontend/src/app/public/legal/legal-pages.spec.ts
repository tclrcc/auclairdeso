import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { provideRouter } from '@angular/router';
import { ConditionsGeneralesPage } from './conditions-generales-page';
import { ConfidentialitePage } from './confidentialite-page';
import { LegalIdentity } from './legal-identity';
import { MentionsLegalesPage } from './mentions-legales-page';

const IDENTITY: LegalIdentity = {
  publisherName: 'Sophie Exemple EI',
  address: '1 place de la Mairie, 01150 Chazey-sur-Ain',
  siret: '123 456 789 00012',
  contactEmail: 'contact@example.com',
  host: 'OVH SAS, Roubaix',
  mediator: null,
  mediatorUrl: null,
};

describe('legal pages', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  async function open<T>(page: Type<T>, identity: LegalIdentity = IDENTITY) {
    const fixture = TestBed.createComponent(page);
    fixture.detectChanges();
    httpTesting.expectOne('/api/legal').flush(identity);
    await fixture.whenStable();
    return fixture;
  }

  it('shows the publisher a visitor must be able to find', async () => {
    const fixture = await open(MentionsLegalesPage);

    const text = (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ');
    expect(text).toContain('Sophie Exemple EI');
    expect(text).toContain('1 place de la Mairie, 01150 Chazey-sur-Ain');
    expect(text).toContain('SIRET : 123 456 789 00012');
    expect(text).toContain('OVH SAS, Roubaix');
  });

  it('says the mediator is still missing rather than inventing one', async () => {
    const fixture = await open(ConditionsGeneralesPage);

    expect(fixture.nativeElement.textContent).toContain('sera indiqué ici avant l’ouverture');
  });

  it('names the mediator once one is chosen', async () => {
    const fixture = await open(ConditionsGeneralesPage, {
      ...IDENTITY,
      mediator: 'Médiation Exemple',
      mediatorUrl: 'https://mediation.example',
    });

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Médiation Exemple');
    expect(text).not.toContain('sera indiqué ici');
    const link = fixture.nativeElement.querySelector('a[href="https://mediation.example"]');
    expect(link).not.toBeNull();
  });

  it('tells exactly what erasing an account keeps', async () => {
    const fixture = await open(ConfidentialitePage);

    const text = (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ');
    expect(text).toContain('Supprimée 7 jours après la séance');
    expect(text).toContain('10 ans');
    expect(text).toContain('n’affiche pas de bandeau');
  });
});
