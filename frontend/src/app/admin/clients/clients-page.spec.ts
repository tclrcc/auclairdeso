import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClientSummary } from '../agenda/admin-appointment';
import { ClientsPage } from './clients-page';

const ALICE: ClientSummary = {
  id: 1,
  email: 'alice@example.com',
  firstName: 'Alice',
  lastName: 'Martin',
  birthDate: '1990-05-04',
  phone: '06 12 34 56 78',
  city: 'Lyon',
  trusted: true,
  blocked: false,
};
const DORA: ClientSummary = { ...ALICE, id: 2, firstName: 'Dora', trusted: false, blocked: true };

describe('ClientsPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClientsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists every client with what sets her apart', async () => {
    const fixture = TestBed.createComponent(ClientsPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/admin/clients?query=&filter=ALL').flush([ALICE, DORA]);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    const links = page.querySelectorAll<HTMLAnchorElement>('li a');
    expect(links).toHaveLength(2);
    expect(links[0].getAttribute('href')).toBe('/admin/clientes/1');
    expect(links[0].textContent).toContain('De confiance');
    expect(links[1].textContent).toContain('Bloquée');
  });

  it('shows only the blocked clients on demand', async () => {
    const fixture = TestBed.createComponent(ClientsPage);
    fixture.detectChanges();
    httpTesting.expectOne('/api/admin/clients?query=&filter=ALL').flush([ALICE, DORA]);
    await fixture.whenStable();

    const blocked = fixture.nativeElement.querySelector('input[value="BLOCKED"]') as HTMLInputElement;
    blocked.click();
    fixture.detectChanges();

    httpTesting.expectOne('/api/admin/clients?query=&filter=BLOCKED').flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Aucune cliente bloquée.');
  });
});
