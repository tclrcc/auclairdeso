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

  it('gives a signed-in client a way to her account, where she logs out', async () => {
    const header = await headerFor({ email: 'alice@example.com', roles: ['CLIENT'], factors: [], passwordSet: false });

    const account = header.querySelector('a[href="/mon-espace#compte"]');
    expect(account?.textContent).toContain('Mon compte');
    expect(header.textContent).not.toContain('Administration');
  });

  it('leads the staff to the administration', async () => {
    const header = await headerFor({ email: 'so@auclairdeso.test', roles: ['PRACTITIONER'], factors: [], passwordSet: true });

    expect(header.querySelector('a[href="/admin"]')?.textContent).toContain('Administration');
  });

  it('unfolds the menu with its button and folds it back with Escape, focus on the button', async () => {
    const fixture = TestBed.createComponent(SiteHeader);
    await fixture.whenStable();
    const header = fixture.nativeElement as HTMLElement;
    const button = header.querySelector<HTMLButtonElement>('button[aria-controls="site-menu"]')!;
    const menu = header.querySelector('#site-menu')!;

    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(menu.getAttribute('data-open')).toBe('false');

    button.click();
    await fixture.whenStable();
    expect(button.getAttribute('aria-expanded')).toBe('true');
    expect(menu.getAttribute('data-open')).toBe('true');

    menu.querySelector('a')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(button);
  });
});
