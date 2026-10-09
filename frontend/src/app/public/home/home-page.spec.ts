import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Offering, OfferingCategory } from '../offerings/offering';
import { HomePage } from './home-page';

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

  it('shows clairvoyance first and leaves out a category without offerings', async () => {
    const page = await render((request) =>
      request.flush([
        offering('magnetisme-30-min', 'Magnétisme — 30 min', 'MAGNETISM'),
        offering('guidance-1-h', 'Guidance — 1 h', 'CLAIRVOYANCE'),
      ]),
    );

    const categories = [...page.querySelectorAll('#seances h3')].map((title) => title.textContent?.trim());
    expect(categories).toEqual(['Voyance', 'Magnétisme']);
  });

  it('links each offering to its booking page', async () => {
    const page = await render((request) =>
      request.flush([offering('guidance-1-h', 'Guidance — 1 h', 'CLAIRVOYANCE')]),
    );

    const link = page.querySelector('a[href="/reserver/guidance-1-h"]');
    expect(link?.textContent).toContain('Guidance — 1 h');
    expect(link?.textContent).toContain('80');
  });

  it('says so when the offerings cannot be loaded', async () => {
    const page = await render((request) =>
      request.flush(null, { status: 500, statusText: 'Internal Server Error' }),
    );

    expect(page.textContent).toContain('ne peuvent pas être affichées');
  });
});
