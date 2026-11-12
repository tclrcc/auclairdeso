import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { VerifyPage } from './verify-page';

describe('VerifyPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'connexion/verifier', component: VerifyPage }]),
        provideLocationMocks(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  async function open(url: string) {
    const harness = await RouterTestingHarness.create(url);
    harness.detectChanges();
    await harness.fixture.whenStable();
    return harness;
  }

  it('reads the token from the fragment, then removes it from the address', async () => {
    const harness = await open('/connexion/verifier?redirect=%2Fmon-espace#token=abc-123');

    expect(TestBed.inject(Location).path(true)).toBe('/connexion/verifier?redirect=%2Fmon-espace');

    harness.routeNativeElement!.querySelector('button')!.click();
    const request = httpTesting.expectOne('/api/auth/magic-link/verify');
    expect(request.request.body.get('token')).toBe('abc-123');
    request.flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  it('asks for a confirmation first, and says the link is invalid only once clicked without a token', async () => {
    const harness = await open('/connexion/verifier');

    expect(harness.routeNativeElement!.textContent).toContain('Dernière étape');

    harness.routeNativeElement!.querySelector('button')!.click();
    harness.detectChanges();

    httpTesting.expectNone('/api/auth/magic-link/verify');
    expect(harness.routeNativeElement!.textContent).toContain('Lien expiré ou déjà utilisé');
  });
});
