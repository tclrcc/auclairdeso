import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';

@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './site-header.html',
})
export class SiteHeader {
  protected readonly auth = inject(AuthSession);
}
