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

  it('leads visitors to their appointments, and the staff to the administration', async () => {
    const fixture = TestBed.createComponent(SiteHeader);
    await fixture.whenStable();
    const header: HTMLElement = fixture.nativeElement;
    expect(header.textContent).toContain('Mes rendez-vous');

    const refreshed = TestBed.inject(AuthSession).refresh();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/me')
      .flush({ email: 'so@auclairdeso.test', roles: ['PRACTITIONER'], factors: [], passwordSet: true });
    await refreshed;
    await fixture.whenStable();

    expect(header.textContent).toContain('Administration');
    expect(header.textContent).not.toContain('Mes rendez-vous');
  });
});
