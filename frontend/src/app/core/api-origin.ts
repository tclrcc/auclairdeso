import { HttpInterceptorFn } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';

/**
 * Where the server-side renderer reaches the backend directly, e.g. http://backend:8080.
 * Provided on the server only: in the browser, /api goes through the public address.
 */
export const API_ORIGIN = new InjectionToken<string>('API_ORIGIN');

export const apiOriginInterceptor: HttpInterceptorFn = (request, next) => {
  const origin = inject(API_ORIGIN, { optional: true });
  return origin && request.url.startsWith('/api/')
    ? next(request.clone({ url: `${origin}${request.url}` }))
    : next(request);
};
