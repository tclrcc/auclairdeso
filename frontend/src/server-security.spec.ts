import {
  contentSecurityPolicy,
  insertNonce,
  NONCE_PLACEHOLDER,
  SECURITY_HEADERS,
  strictTransportSecurity,
} from './server-security';

describe('server security headers', () => {
  it('allows only this site, plus the inline code that carries the nonce', () => {
    const policy = contentSecurityPolicy('abc123');

    expect(policy).toContain("default-src 'self'");
    expect(policy).toContain("script-src 'self' 'nonce-abc123'");
    expect(policy).toContain("style-src 'self' 'nonce-abc123'");
    expect(policy).toContain("frame-ancestors 'none'");
    expect(policy).not.toContain('unsafe-inline');
    expect(policy).not.toContain('http');
  });

  it('replaces every placeholder with the nonce of the response', () => {
    const html = `<app-root ngCspNonce="${NONCE_PLACEHOLDER}"></app-root><style nonce="${NONCE_PLACEHOLDER}"></style>`;

    expect(insertNonce(html, 'xyz')).toBe('<app-root ngCspNonce="xyz"></app-root><style nonce="xyz"></style>');
  });

  it('never sends the address of a page to another site', () => {
    expect(SECURITY_HEADERS['Referrer-Policy']).toBe('no-referrer');
    expect(SECURITY_HEADERS['X-Content-Type-Options']).toBe('nosniff');
  });

  it('keeps HSTS off until HSTS_MAX_AGE holds a number of seconds', () => {
    expect(strictTransportSecurity(undefined)).toBeNull();
    expect(strictTransportSecurity('')).toBeNull();
    expect(strictTransportSecurity('one year')).toBeNull();
    expect(strictTransportSecurity(' 300 ')).toBe('max-age=300');
  });
});
