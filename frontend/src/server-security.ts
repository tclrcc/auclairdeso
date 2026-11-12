/**
 * Security headers of the rendering server (server.ts), kept apart so that they can be tested
 * without starting the server.
 */

/**
 * Written on <app-root ngCspNonce="..."> in index.html. The build copies it onto every inline
 * <script> and <style>, and Angular reads it at runtime for the styles it adds later. Each HTML
 * response replaces it with a fresh nonce: never remove it from index.html.
 */
export const NONCE_PLACEHOLDER = 'NG_CSP_NONCE';

/** The Content Security Policy of a response whose inline scripts and styles carry this nonce. */
export function contentSecurityPolicy(nonce: string): string {
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}'`,
    `style-src 'self' 'nonce-${nonce}'`,
    // blob: for the preview of the photo chosen on the booking page (URL.createObjectURL).
    "img-src 'self' data: blob:",
    "media-src 'self'",
    "font-src 'self'",
    "connect-src 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join('; ');
}

/** Set on every response of the rendering server, whatever its type. */
export const SECURITY_HEADERS: Readonly<Record<string, string>> = {
  // Magic links carry a token: the address of a page is never sent to another site.
  'Referrer-Policy': 'no-referrer',
  'X-Content-Type-Options': 'nosniff',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
};

/** Puts the nonce of this response in place of the placeholder. */
export function insertNonce(html: string, nonce: string): string {
  return html.replaceAll(NONCE_PLACEHOLDER, nonce);
}

/**
 * The Strict-Transport-Security value for HSTS_MAX_AGE (seconds), or null when it is unset or not a
 * number: HSTS stays off until the definitive domain is served over HTTPS (deploy/.env.example).
 */
export function strictTransportSecurity(maxAge: string | undefined): string | null {
  const seconds = maxAge?.trim();
  return seconds && /^\d+$/.test(seconds) ? `max-age=${seconds}` : null;
}
