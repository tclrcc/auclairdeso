import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { todayInParis } from '../../shared/paris-time';
import { Bill, ReceiptBook } from './bill';
import { ReceiptsPage } from './receipts-page';

const BILL: Bill = {
  id: 3,
  number: '2026-0007',
  appointmentId: 41,
  issuedOn: '2026-10-16',
  practitionerName: 'Sophie Exemple EI',
  practitionerAddress: '1 place de la Mairie, 01150 Chazey-sur-Ain',
  siret: '123 456 789 00012',
  clientName: 'Alice Martin',
  clientEmail: 'alice@example.com',
  serviceStart: '2026-10-16T08:00:00Z',
  servicePlace: 'À distance, en visio',
  description: 'Consultation approfondie',
  amountCents: 8000,
  vatMention: 'TVA non applicable, article 293 B du CGI',
  paymentMethod: null,
  paidOn: null,
};

const normalise = (value: string) => value.replace(/\s/g, ' ');
const YEAR = Number(todayInParis().slice(0, 4));

const BOOK: ReceiptBook = {
  year: YEAR,
  entries: [
    {
      billId: 1,
      appointmentId: 10,
      paidOn: `${YEAR}-10-02`,
      number: `${YEAR}-0001`,
      clientName: 'Chloé Bernard',
      description: 'Coupage de feu',
      method: 'MOBILE',
      amountCents: 4000,
    },
  ],
  totalCents: 4000,
  totalsByMethod: { TRANSFER: 0, CASH: 0, MOBILE: 4000 },
};

describe('ReceiptsPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReceiptsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists the bills to collect, then the receipt book of the year', async () => {
    const fixture = TestBed.createComponent(ReceiptsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/admin/bills/unpaid').flush([BILL]);
    httpTesting.expectOne(`/api/admin/receipts?year=${YEAR}`).flush(BOOK);
    await fixture.whenStable();

    const text = normalise(fixture.nativeElement.textContent);
    expect(text).toContain('Alice Martin');
    expect(text).toContain('note n° 2026-0007');
    expect(text).toContain('Chloé Bernard, Coupage de feu');
    expect(text).toContain('40,00 €');
    const download = fixture.nativeElement.querySelector('a[download]') as HTMLAnchorElement;
    expect(download.getAttribute('href')).toBe(`/api/admin/receipts/export?year=${YEAR}`);
  });

  it('goes back to the previous year, never beyond the current one', async () => {
    const fixture = TestBed.createComponent(ReceiptsPage);
    fixture.detectChanges();
    httpTesting.expectOne('/api/admin/bills/unpaid').flush([]);
    httpTesting.expectOne(`/api/admin/receipts?year=${YEAR}`).flush(BOOK);
    await fixture.whenStable();

    const next = fixture.nativeElement.querySelector('[aria-label="Année suivante"]') as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    (fixture.nativeElement.querySelector('[aria-label="Année précédente"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    httpTesting.expectOne(`/api/admin/receipts?year=${YEAR - 1}`).flush({ ...BOOK, year: YEAR - 1, entries: [] });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain(`Aucun encaissement en ${YEAR - 1}.`);
  });
});
