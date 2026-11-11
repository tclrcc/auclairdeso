import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SiteFooter } from './site-footer';
import { SiteHeader } from './site-header';

/**
 * The frame of every public page: header, page, footer, on the starry night of night-theme.css.
 * Administration screens keep their own frame, and the light theme.
 */
@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, SiteHeader, SiteFooter],
  host: { class: 'night-theme' },
  template: `
    <app-site-header />
    <router-outlet />
    <app-site-footer class="mt-auto" />
  `,
})
export class PublicLayout {}
