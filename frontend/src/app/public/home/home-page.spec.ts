import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Offering, OfferingCategory } from '../offerings/offering';
import { aboutPreviewOf, HomePage } from './home-page';

function offering(slug: string, name: string, category: OfferingCategory): Offering {
  return {
    slug,
    name,
    description: '',
    durationMinutes: 60,
    bufferMinutes: 15,
    priceCents: 8000,
    paymentPolicy: 'ON_SITE',
    depositCents: null,
    modes: ['VIDEO'],
    category,
    videoPath: null,
    videoTranscript: null,
  };
}

describe('HomePage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** Renders the page, answers its request for the offerings, and returns its HTML. */
  async function render(answer: (request: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(HomePage);
    TestBed.tick();
    answer(httpTesting.expectOne('/api/offerings'));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('leads to the offerings and to So from the first screen', async () => {
    const page = await render((request) => request.flush([]));

    expect(page.querySelector('h1')?.textContent).toContain('Une présence à l’écoute, pour y voir plus clair');
    expect(page.querySelector('a[href="/seances"]')?.textContent).toContain('Voir les séances');
    expect(page.querySelector('a[href="/a-propos"]')?.textContent).toContain('Découvrir So');
  });

  it('shows clairvoyance first, each offering under its category', async () => {
    const page = await render((request) =>
      request.flush([
        offering('magnetisme-30-min', 'Magnétisme — 30 min', 'MAGNETISM'),
        offering('guidance-1-h', 'Guidance — 1 h', 'CLAIRVOYANCE'),
      ]),
    );

    const cards = [...page.querySelectorAll('#seances li')];
    expect(cards.map((card) => card.querySelector('h3')?.textContent?.trim())).toEqual([
      'Guidance — 1 h',
      'Magnétisme — 30 min',
    ]);
    expect(cards.map((card) => card.querySelector('p')?.textContent?.trim())).toEqual(['Voyance', 'Magnétisme']);
  });

  it('links each offering to its booking page and shows its duration and price', async () => {
    const page = await render((request) =>
      request.flush([offering('guidance-1-h', 'Guidance — 1 h', 'CLAIRVOYANCE')]),
    );

    const link = page.querySelector('a[href="/reserver/guidance-1-h"]');
    expect(link?.textContent).toContain('Guidance — 1 h');
    const card = link?.closest('li');
    expect(card?.textContent).toContain('1 h');
    expect(card?.textContent).toContain('80');
  });

  it('says so when the offerings cannot be loaded', async () => {
    const page = await render((request) =>
      request.flush(null, { status: 500, statusText: 'Internal Server Error' }),
    );

    expect(page.textContent).toContain('ne peuvent pas être affichées');
  });

  it('describes the three steps of a booking', async () => {
    const page = await render((request) => request.flush([]));

    const steps = [...page.querySelectorAll('ol h3')].map((title) => title.textContent?.trim());
    expect(steps).toEqual(['Choisissez votre séance', 'Demandez un créneau', 'Recevez la confirmation']);
  });

  it('leaves out the card about So while her portrait and her path are not written', async () => {
    const page = await render((request) => request.flush([]));

    expect(page.textContent).not.toContain('Faire connaissance avec So');
  });
});

describe('aboutPreviewOf', () => {
  it('needs both a portrait and a written path', () => {
    expect(aboutPreviewOf(null, ['Mon parcours.'])).toBeNull();
    expect(aboutPreviewOf('/contenus/so.webp', [])).toBeNull();
    expect(aboutPreviewOf('/contenus/so.webp', ['Mon parcours.', 'La suite.'])).toEqual({
      portrait: '/contenus/so.webp',
      excerpt: 'Mon parcours.',
    });
  });
});
