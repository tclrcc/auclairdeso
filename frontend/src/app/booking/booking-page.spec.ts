import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
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
