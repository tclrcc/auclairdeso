import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_ORIGIN, apiOriginInterceptor } from './api-origin';

describe('apiOriginInterceptor', () => {
  function setUp(origin?: string) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiOriginInterceptor])),
        provideHttpClientTesting(),
        ...(origin ? [{ provide: API_ORIGIN, useValue: origin }] : []),
      ],
    });
    return { http: TestBed.inject(HttpClient), httpTesting: TestBed.inject(HttpTestingController) };
  }

  it('sends /api calls straight to the backend on the server', () => {
    const { http, httpTesting } = setUp('http://backend:8080');

    http.get('/api/offerings').subscribe();

    httpTesting.expectOne('http://backend:8080/api/offerings');
  });

  it('leaves requests alone in the browser', () => {
    const { http, httpTesting } = setUp();

    http.get('/api/offerings').subscribe();

    httpTesting.expectOne('/api/offerings');
  });
});
