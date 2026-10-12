import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { todayInParis } from '../../shared/paris-time';
import { Bill } from './bill';
import { BillPanel } from './bill-panel';

/** Lets pending promises (the form submission) run before asserting. */
const settle = () => new Promise<void>((resolve) => setTimeout(resolve));

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

describe('BillPanel', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BillPanel],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  async function open(bills: Bill[]) {
    const fixture = TestBed.createComponent(BillPanel);
    fixture.componentRef.setInput('appointmentId', 41);
    fixture.detectChanges();
    httpTesting.expectOne('/api/admin/bills?appointmentId=41').flush(bills);
    await fixture.whenStable();
    return fixture;
  }

  it('records a payment in cash, dated today by default', async () => {
    const fixture = await open([BILL]);
    const page: HTMLElement = fixture.nativeElement;
    expect(page.textContent).toContain('Note n° 2026-0007');

    (page.querySelector('input[value="CASH"]') as HTMLInputElement).click();
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    const request = httpTesting.expectOne('/api/admin/bills/3/payment');
    expect(request.request.body).toEqual({ method: 'CASH', paidOn: todayInParis() });
    request.flush({ ...BILL, paymentMethod: 'CASH', paidOn: '2026-10-18' });
    await settle();
    await fixture.whenStable();

    expect(page.textContent).toContain('Encaissée le 18 octobre 2026 en espèces.');
  });

  it('asks for the payment method before sending anything', async () => {
    const fixture = await open([BILL]);

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await settle();
    await fixture.whenStable();

    httpTesting.expectNone('/api/admin/bills/3/payment');
    expect(fixture.nativeElement.textContent).toContain('Choisissez le moyen de paiement.');
  });

  it('says when a session has no bill', async () => {
    const fixture = await open([]);

    expect(fixture.nativeElement.textContent).toContain('Aucune note pour cette séance.');
  });
});
