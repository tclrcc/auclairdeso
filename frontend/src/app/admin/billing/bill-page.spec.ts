import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Bill } from './bill';
import { BillPage } from './bill-page';

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

const normalise = (value: string) => value.replace(/\s+/g, ' ');

describe('BillPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BillPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('prints every mention the law asks for', async () => {
    const fixture = TestBed.createComponent(BillPage);
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    httpTesting.expectOne('/api/admin/bills/3').flush(BILL);
    await fixture.whenStable();

    const text = normalise(fixture.nativeElement.textContent);
    expect(text).toContain('Note n° 2026-0007');
    expect(text).toContain('Établie le 16 octobre 2026');
    expect(text).toContain('Sophie Exemple EI');
    expect(text).toContain('1 place de la Mairie, 01150 Chazey-sur-Ain');
    expect(text).toContain('SIRET : 123 456 789 00012');
    expect(text).toContain('Alice Martin');
    expect(text).toContain('vendredi 16 octobre 2026 à 10:00');
    expect(text).toContain('À distance, en visio');
    // Angular drops the spaces between two tags: the label and its amount touch in textContent.
    expect(text).toContain('Total à payer80,00 €');
    expect(text).toContain('TVA non applicable, article 293 B du CGI');
  });
});
