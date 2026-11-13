import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';
import { Offering } from '../public/offerings/offering';
import { BookingPage } from './booking-page';

const GUIDANCE: Offering = {
  slug: 'guidance-1-h',
  name: 'Guidance — 1 h',
  description: 'Une séance complète.',
  durationMinutes: 60,
  bufferMinutes: 15,
  priceCents: 8000,
  paymentPolicy: 'ON_SITE',
  depositCents: null,
  modes: ['VIDEO', 'PHONE'],
  category: 'CLAIRVOYANCE',
  videoPath: null,
  videoTranscript: null,
};

describe('BookingPage video', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BookingPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  async function open(offering: Offering): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(BookingPage);
    fixture.componentRef.setInput('slug', offering.slug);
    fixture.detectChanges();
    httpTesting.expectOne(`/api/offerings/${offering.slug}`).flush(offering);
    // The slot picker only appears once the offering is there, and then asks for the week's slots:
    // not what is tested here, but nothing may stay pending.
    for (let turn = 0; turn < 3; turn++) {
      fixture.detectChanges();
      await Promise.resolve();
      httpTesting.match((request) => request.url.includes('/slots')).forEach((request) => request.flush([]));
    }
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('shows the booking steps and no video for an offering without one', async () => {
    const page = await open(GUIDANCE);

    expect(page.querySelector('h1')?.textContent).toContain('Guidance — 1 h');
    expect(page.querySelector('video')).toBeNull();
    expect(page.querySelector('details')).toBeNull();
  });

  it('plays the video of the offering and offers its transcript', async () => {
    const page = await open({
      ...GUIDANCE,
      videoPath: 'guidance-1-h.mp4',
      videoTranscript: 'Bonjour, je suis So.',
    });

    const video = page.querySelector('video');
    expect(video?.getAttribute('src')).toBe('/contenus/guidance-1-h.mp4');
    expect(video?.getAttribute('preload')).toBe('metadata');
    expect(page.querySelector('details summary')?.textContent).toContain('Lire la transcription');
    expect(page.querySelector('details')?.textContent).toContain('Bonjour, je suis So.');
  });
});

describe('BookingPage steps', () => {
  /** Friday 13 November 2026, 11:00 in Paris. */
  const NOW = new Date('2026-11-13T10:00:00Z');
  const MAGNETISM: Offering = {
    ...GUIDANCE,
    slug: 'magnetisme',
    name: 'Magnétisme',
    durationMinutes: 45,
    priceCents: 6000,
    modes: ['CLIENT_HOME', 'PHONE'],
    category: 'MAGNETISM',
  };
  const SLOT = { start: '2026-11-19T14:00:00+01:00', end: '2026-11-19T14:45:00+01:00' };
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(NOW);
    TestBed.configureTestingModule({
      imports: [BookingPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.useRealTimers());

  /** Opens the page as an anonymous visitor, or logged in, with one free slot on Thursday 19. */
  async function open(loggedIn: boolean) {
    const refreshed = TestBed.inject(AuthSession).refresh();
    const me = httpTesting.expectOne('/api/me');
    if (loggedIn) {
      me.flush({ email: 'alice@example.com', roles: ['CLIENT'], factors: ['OTT'], passwordSet: false });
    } else {
      me.flush(null, { status: 401, statusText: 'Unauthorized' });
    }
    await refreshed;

    const fixture = TestBed.createComponent(BookingPage);
    fixture.componentRef.setInput('slug', MAGNETISM.slug);
    fixture.detectChanges();
    httpTesting.expectOne(`/api/offerings/${MAGNETISM.slug}`).flush(MAGNETISM);
    for (let turn = 0; turn < 3; turn++) {
      fixture.detectChanges();
      // Not whenStable(): a request still waiting for its answer keeps the application unstable.
      await Promise.resolve();
      httpTesting.match((request) => request.url.includes('/slots')).forEach((request) => request.flush([SLOT]));
      httpTesting.match('/api/bookings/profile').forEach((request) => request.flush(null));
    }
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    return { fixture, page };
  }

  async function choose(fixture: { detectChanges(): void; whenStable(): Promise<unknown> }, page: HTMLElement) {
    page.querySelectorAll<HTMLInputElement>('input[name="mode"]')[1].click(); // Par téléphone
    page.querySelector<HTMLElement>('[data-date="2026-11-19"]')!.click();
    fixture.detectChanges();
    page.querySelector<HTMLInputElement>('input[name="slot"]')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function summaries(page: HTMLElement): string[] {
    return [...page.querySelectorAll('app-booking-summary')].map((summary) => summary.textContent!.replace(/\s+/g, ' '));
  }

  it('says from the start that So reads and validates each request herself', async () => {
    const { page } = await open(false);

    expect(page.textContent).toContain('So lit et valide chaque demande elle-même.');
    expect(page.textContent).toContain('Le créneau vous est gardé en attendant sa réponse.');
  });

  it('sums up the request before the login, then with each choice', async () => {
    const { fixture, page } = await open(false);

    // One summary beside the steps (large screens), one just before the login button (phones).
    expect(summaries(page)).toHaveLength(2);
    expect(summaries(page)[0]).toContain('Magnétisme · 45 min');
    expect(summaries(page)[0]).toContain('60 €, après la séance');
    expect(summaries(page)[0].match(/À choisir/g)).toHaveLength(2);

    await choose(fixture, page);

    for (const summary of summaries(page)) {
      expect(summary).toContain('Par téléphone');
      expect(summary).toContain('jeudi 19 novembre à 14:00');
      expect(summary).not.toContain('À choisir');
    }
  });

  it('sends the client back to a step from the summary, its title taking the focus', async () => {
    const { fixture, page } = await open(false);
    await choose(fixture, page);
    Element.prototype.scrollIntoView = vi.fn();

    const change = [...page.querySelectorAll('app-booking-summary button')]
      .find((button) => button.textContent?.includes("la date et l'heure")) as HTMLButtonElement;
    change.click();

    expect(document.activeElement?.id).toBe('step-slot');
  });

  it('shows the request as waiting for So once sent, never as confirmed', async () => {
    const { fixture, page } = await open(true);
    await choose(fixture, page);

    const fill = (selector: string, value: string) => {
      const field = page.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
      field.value = value;
      field.dispatchEvent(new Event('input'));
    };
    fill('#first-name', 'Alice');
    fill('#last-name', 'Martin');
    fill('#birth-date', '1990-04-12');
    fill('#phone', '06 12 34 56 78');
    fill('#reason', 'Un moment pour souffler.');
    page.querySelectorAll<HTMLInputElement>('form input[type="checkbox"]').forEach((box) => box.click());
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
    httpTesting.expectOne('/api/bookings').flush({
      id: 7,
      offeringSlug: MAGNETISM.slug,
      offeringName: MAGNETISM.name,
      mode: 'PHONE',
      start: SLOT.start,
      end: SLOT.end,
      status: 'REQUESTED',
      priceCents: 6000,
      cancellationDeadline: '2026-11-17T14:00:00+01:00',
      lateCancellation: false,
    });
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    const title = page.querySelector('h1')!;
    expect(title.textContent).toContain('Votre demande est envoyée');
    expect(document.activeElement).toBe(title);
    const text = page.textContent!.replace(/\s+/g, ' ');
    expect(text).toContain('En attente de validation par So');
    expect(text).toContain('Le créneau du jeudi 19 novembre à 14:00 vous est gardé');
    expect(text).toContain("Vous recevez sa réponse par e-mail, qu'elle accepte ou non la demande");
    expect(text).toContain('la veille de la séance à 21 h, la demande expire et le créneau est libéré');
    expect(text).not.toMatch(/confirmé/i);
  });
});
