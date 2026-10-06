import { Component, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthSession } from '../../core/auth/auth-session';

interface StaffMember {
  readonly email: string;
  readonly role: 'PRACTITIONER' | 'ADMIN';
  readonly passwordSet: boolean;
}

@Component({
  selector: 'app-admin-home-page',
  imports: [RouterLink],
  templateUrl: './admin-home-page.html',
})
export class AdminHomePage {
  protected readonly auth = inject(AuthSession);
  protected readonly staff = httpResource<StaffMember[]>(() => '/api/admin/staff');
}
