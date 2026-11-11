import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OfferingsPage } from './offerings-page';
import { Offering } from './offering';
import {provideRouter, RouterLink} from '@angular/router';

const GUIDANCE: Offering = {
  slug: 'guidance-1-h',
  name: 'Guidance — 1 h',
  description: 'Une séance complète.',
  durationMinutes: 60,
  bufferMinutes: 15,
  priceCents: 8000,
  paymentPolicy: 'DEPOSIT_ONLINE',
  depositCents: 3000,
  modes: ['IN_PERSON', 'VIDEO', 'PHONE'],
  category: 'CLAIRVOYANCE',
  videoPath: null,
  videoTranscript: null,
};

describe('OfferingsPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OfferingsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should list offerings with their remote modes, and never the premises or an online payment', async () => {
    const fixture = TestBed.createComponent(OfferingsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/offerings').flush([GUIDANCE]);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelectorAll('article')).toHaveLength(1);
    expect(page.querySelector('h2')?.textContent).toContain('Guidance — 1 h');
    expect(page.textContent).toContain('Par téléphone');
    expect(page.textContent).not.toContain('En présentiel');
    expect(page.textContent).toContain('Le règlement se fait après la séance');
    expect(page.textContent).toContain("Aucun paiement n'est demandé en ligne.");
    expect(page.textContent).toContain('Voyance');
    expect(page.textContent).toContain('1 h');
    const booking = page.querySelector<HTMLAnchorElement>('a[href="/reserver/guidance-1-h"]');
    expect(booking?.textContent).toContain('Demander un créneau');
    expect(page.textContent).not.toContain('Acompte');
    expect(page.textContent).not.toContain('en ligne à la réservation');
  });

  it('should not draw an empty list of modes when only the premises are offered', async () => {
    const fixture = TestBed.createComponent(OfferingsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/offerings').flush([{ ...GUIDANCE, modes: ['IN_PERSON'] }]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[aria-label="Modes de consultation"]')).toBeNull();
  });

  it('should show an alert when the API fails', async () => {
    const fixture = TestBed.createComponent(OfferingsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/offerings').flush('Boom', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });

  describe('video', () => {
    async function openWith(offering: Offering) {
      const fixture = TestBed.createComponent(OfferingsPage);
      fixture.detectChanges();
      httpTesting.expectOne('/api/offerings').flush([offering]);
      await fixture.whenStable();
      return fixture.nativeElement as HTMLElement;
    }

    it('shows nothing about a video when the offering has none', async () => {
      const page = await openWith(GUIDANCE);

      expect(page.querySelector('video')).toBeNull();
      expect(page.querySelector('details')).toBeNull();
    });

    it('plays the video of the offering with the player of the about page', async () => {
      const page = await openWith({ ...GUIDANCE, videoPath: 'guidance-1-h.mp4' });

      const video = page.querySelector('video');
      expect(video?.getAttribute('src')).toBe('/contenus/guidance-1-h.mp4');
      expect(video?.getAttribute('preload')).toBe('metadata');
      expect(video?.hasAttribute('controls')).toBe(true);
      expect(video?.hasAttribute('autoplay')).toBe(false);
      expect(video?.getAttribute('aria-label')).toBe('Guidance — 1 h');
    });

    it('offers the transcript under the video in a block that starts closed', async () => {
      const page = await openWith({
        ...GUIDANCE,
        videoPath: 'guidance-1-h.mp4',
        videoTranscript: 'Bonjour, je suis So.',
      });

      const details = page.querySelector('details');
      expect(details?.querySelector('summary')?.textContent).toContain('Lire la transcription');
      expect(details?.textContent).toContain('Bonjour, je suis So.');
      expect(details?.open).toBe(false);
    });

    it('shows the video without a transcript block while none is written', async () => {
      const page = await openWith({ ...GUIDANCE, videoPath: 'guidance-1-h.mp4' });

      expect(page.querySelector('video')).not.toBeNull();
      expect(page.querySelector('details')).toBeNull();
    });
  });
});
