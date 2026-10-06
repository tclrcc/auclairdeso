import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthSession } from './auth-session';

describe('AuthSession', () => {
  let auth: AuthSession;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthSession);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('treats a 401 on /api/me as an anonymous visitor', async () => {
    const result = auth.refresh();
    httpTesting.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('sends the email as a form field', async () => {
    const result = auth.requestMagicLink('client@example.com');
    const request = httpTesting.expectOne('/api/auth/magic-link');

    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('username')).toBe('client@example.com');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await result;
  });

  it('reports an expired or reused token', async () => {
    const result = auth.verify('used-token');
    httpTesting.expectOne('/api/auth/magic-link/verify')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBe(false);
  });
});
