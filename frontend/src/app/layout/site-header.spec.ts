import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';
import { SiteHeader } from './site-header';

describe('SiteHeader', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SiteHeader],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  async function headerFor(user: object) {
    const fixture = TestBed.createComponent(SiteHeader);
    const refreshed = TestBed.inject(AuthSession).refresh();
    TestBed.inject(HttpTestingController).expectOne('/api/me').flush(user);
    await refreshed;
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('invites a visitor to log in', async () => {
    const fixture = TestBed.createComponent(SiteHeader);
    await fixture.whenStable();

    const link = fixture.nativeElement.querySelector('a[href="/connexion"]');
    expect(link?.textContent).toContain('Se connecter');
  });

  it('leads a client to her appointments', async () => {
    const header = await headerFor({ email: 'alice@example.com', roles: ['CLIENT'], factors: [], passwordSet: false });

    expect(header.querySelector('a[href="/mon-espace"]')?.textContent).toContain('Mes rendez-vous');
    expect(header.textContent).not.toContain('Se connecter');
  });

  it('leads the staff to the administration', async () => {
    const header = await headerFor({ email: 'so@auclairdeso.test', roles: ['PRACTITIONER'], factors: [], passwordSet: true });

    expect(header.querySelector('a[href="/admin"]')?.textContent).toContain('Administration');
  });
});
