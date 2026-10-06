import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginPage } from './login-page';

/** Lets pending promises (the form submission) run before asserting. */
const settle = () => new Promise<void>((resolve) => setTimeout(resolve));

describe('LoginPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  async function submitEmail(value: string) {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await settle();
    return fixture;
  }

  it('should not send anything for an invalid email', async () => {
    const fixture = await submitEmail('pas-un-email');

    httpTesting.expectNone('/api/auth/magic-link');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('ne semble pas valide');
  });

  it('should request a magic link and confirm it was sent', async () => {
    const fixture = await submitEmail('Client@Example.com');

    httpTesting.expectOne('/api/auth/magic-link').flush(null, { status: 204, statusText: 'No Content' });
    await settle();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Vérifiez votre boîte mail');
  });
});
