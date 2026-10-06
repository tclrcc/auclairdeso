import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OfferingsPage } from './offerings-page';
import { Offering } from './offering';

const GUIDANCE: Offering = {
  slug: 'guidance-1-h',
  name: 'Guidance — 1 h',
  description: 'Une séance complète.',
  durationMinutes: 60,
  priceCents: 8000,
  paymentPolicy: 'DEPOSIT_ONLINE',
  depositCents: 3000,
  modes: ['VIDEO', 'PHONE'],
};

describe('OfferingsPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OfferingsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should list offerings with their modes and deposit', async () => {
    const fixture = TestBed.createComponent(OfferingsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/offerings').flush([GUIDANCE]);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelectorAll('article')).toHaveLength(1);
    expect(page.querySelector('h2')?.textContent).toContain('Guidance — 1 h');
    expect(page.textContent).toContain('Par téléphone');
    expect(page.textContent).toContain('Acompte de 30');
  });

  it('should show an alert when the API fails', async () => {
    const fixture = TestBed.createComponent(OfferingsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/offerings').flush('Boom', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });
});
