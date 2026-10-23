import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClientFile } from './admin-client';
import { ClientFilePage } from './client-file-page';

/** Lets pending promises (an action, a submission) run before asserting. */
const settle = () => new Promise<void>((resolve) => setTimeout(resolve));

const FILE: ClientFile = {
  client: {
    id: 7,
    email: 'alice@example.com',
    firstName: 'Alice',
    lastName: 'Martin',
    birthDate: '1990-05-04',
    phone: '06 12 34 56 78',
    city: 'Lyon',
    trusted: false,
    blocked: true,
    erased: false,
  },
  bookedOnline: true,
  phoneBlocked: false,
  lateCancellations: 2,
  lateCancellationsBeforeBlock: 2,
  appointments: [
    {
      id: 31,
      start: '2026-10-12T12:00:00Z',
      offeringName: 'Consultation approfondie',
      mode: 'VIDEO',
      status: 'CANCELLED',
      priceCents: 8000,
      lateCancellation: true,
    },
  ],
  notes: [{ id: 3, text: 'Préfère être appelée le matin.', createdAt: '2026-10-01T08:30:00Z' }],
};

describe('ClientFilePage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClientFilePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
  });

  async function open(file: ClientFile) {
    const fixture = TestBed.createComponent(ClientFilePage);
    fixture.componentRef.setInput('id', '7');
    fixture.detectChanges();
    httpTesting.expectOne('/api/admin/clients/7').flush(file);
    await fixture.whenStable();
    return fixture;
  }

  function button(page: HTMLElement, label: string): HTMLButtonElement {
    const found = [...page.querySelectorAll('button')].find((candidate) => candidate.textContent?.trim() === label);
    if (!found) {
      throw new Error(`No button "${label}"`);
    }
    return found;
  }

  it('shows the notes, the history and the late cancellations', async () => {
    const fixture = await open(FILE);

    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelector('h1')?.textContent).toContain('Alice Martin');
    expect(page.textContent).toContain('Préfère être appelée le matin.');
    expect(page.textContent).toContain('Annulée tardivement');
    expect(page.querySelector('[aria-label="2 sur 2"]')).not.toBeNull();
  });

  it('explains a block that comes from the phone number of another client', async () => {
    const fixture = await open({ ...FILE, client: { ...FILE.client, blocked: false }, phoneBlocked: true });

    expect(fixture.nativeElement.textContent).toContain('aussi celui d’une cliente bloquée');
  });

  it('unblocks the client once the practitioner confirms', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = await open(FILE);

    button(fixture.nativeElement, 'Débloquer').click();
    await settle();

    const request = httpTesting.expectOne('/api/admin/clients/7/unblock');
    expect(request.request.method).toBe('POST');
    request.flush({ ...FILE, client: { ...FILE.client, blocked: false }, lateCancellations: 0 });
    await settle();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).not.toContain('Réservation en ligne bloquée');
    expect(fixture.nativeElement.querySelector('[aria-label="0 sur 2"]')).not.toBeNull();
  });

  it('never lets the login email of an online client be changed', async () => {
    const fixture = await open(FILE);

    button(fixture.nativeElement, 'Modifier').click();
    await fixture.whenStable();

    const email = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
    expect(email.value).toBe('alice@example.com');
    expect(email.readOnly).toBe(true);
  });
});
