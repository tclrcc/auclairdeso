import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SiteFooter } from './site-footer';
import { SiteHeader } from './site-header';

/** The frame of every public page: header, page, footer. Administration screens keep their own. */
@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, SiteHeader, SiteFooter],
  template: `
    <app-site-header />
    <router-outlet />
    <app-site-footer />
  `,
})
export class PublicLayout {}
