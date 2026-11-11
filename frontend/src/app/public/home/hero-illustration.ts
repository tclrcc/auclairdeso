import { Component } from '@angular/core';

/**
 * The moon of the home page: a violet halo, two gold circles, a crescent and a few stars.
 * Purely decorative (aria-hidden), static, drawn in inline SVG: no image to download.
 */
@Component({
  selector: 'app-hero-illustration',
  host: { class: 'block' },
  template: `
    <svg viewBox="0 0 440 440" aria-hidden="true" focusable="false" class="size-full">
      <defs>
        <radialGradient id="hero-halo">
          <stop offset="0" stop-color="#6a3fb5" stop-opacity="0.55" />
          <stop offset="1" stop-color="#6a3fb5" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="hero-moon" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#f6e7b4" />
          <stop offset="1" stop-color="#d9b25a" />
        </linearGradient>
      </defs>

      <circle cx="220" cy="220" r="215" fill="url(#hero-halo)" />
      <circle cx="220" cy="220" r="170" fill="none" stroke="#e8c872" stroke-opacity="0.35" />
      <circle cx="220" cy="220" r="205" fill="none" stroke="#e8c872" stroke-opacity="0.18" stroke-dasharray="2 7" />

      <path d="M250 96a124 124 0 1 0 66 228A104 104 0 0 1 250 96z" fill="url(#hero-moon)" />

      <g fill="#f6e7b4">
        <path d="M118 92q2 10 12 12q-10 2-12 12q-2-10-12-12q10-2 12-12z" />
        <path d="M338 150q1.5 7 8.5 8.5q-7 1.5-8.5 8.5q-1.5-7-8.5-8.5q7-1.5 8.5-8.5z" />
        <path d="M352 318q2 9 11 11q-9 2-11 11q-2-9-11-11q9-2 11-11z" />
      </g>
      <circle cx="92" cy="292" r="2" fill="#f6e7b4" />
      <circle cx="302" cy="64" r="1.6" fill="#cdbdf0" />
      <circle cx="182" cy="384" r="1.8" fill="#cdbdf0" />
    </svg>
  `,
})
export class HeroIllustration {}
