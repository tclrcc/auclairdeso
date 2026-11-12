import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { randomBytes } from 'node:crypto';
import { join } from 'node:path';
import {
  contentSecurityPolicy,
  insertNonce,
  SECURITY_HEADERS,
  strictTransportSecurity,
} from './server-security';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
app.disable('x-powered-by');
// For req.secure (HSTS below): X-Forwarded-Proto is believed only from a private network, the gateway's.
app.set('trust proxy', 'uniquelocal');

/**
 * Only the gateway reaches this server: port 4000 is published nowhere (deploy/compose.yaml), and the
 * gateway always sets X-Forwarded-Proto. Trusting that header, and only that one, lets the rendered
 * application know whether the visitor came over HTTP (SSH tunnel) or HTTPS (nginx of the VPS).
 * Any other X-Forwarded-* header a visitor might send is ignored.
 */
const angularApp = new AngularNodeAppEngine({ trustProxyHeaders: ['x-forwarded-proto'] });

/** Off until HSTS_MAX_AGE is set (deploy/.env.example). */
const hsts = strictTransportSecurity(process.env['HSTS_MAX_AGE']);

/**
 * Security headers on every response, with a fresh nonce for the inline scripts and styles of the page.
 */
app.use((req, res, next) => {
  const nonce = randomBytes(16).toString('base64');
  res.locals['cspNonce'] = nonce;
  res.setHeader('Content-Security-Policy', contentSecurityPolicy(nonce));
  for (const [name, value] of Object.entries(SECURITY_HEADERS)) {
    res.setHeader(name, value);
  }
  if (hsts && req.secure) {
    res.setHeader('Strict-Transport-Security', hsts);
  }
  next();
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  // Each page carries its own nonce: never answer 304 and let the browser reuse an older copy.
  delete req.headers['if-none-match'];
  angularApp
    .handle(req)
    .then(async (response) =>
      response
        ? writeResponseToNodeResponse(await withNonce(response, res.locals['cspNonce']), res)
        : next(),
    )
    .catch(next);
});

/** The HTML of the page with the nonce of this response, which no cache may keep or revalidate. */
async function withNonce(response: Response, nonce: string): Promise<Response> {
  if (!response.headers.get('content-type')?.startsWith('text/html')) {
    return response;
  }
  const headers = new Headers(response.headers);
  headers.delete('content-length');
  headers.delete('etag');
  headers.set('cache-control', 'no-cache');
  return new Response(insertNonce(await response.text(), nonce), {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
