import { Component } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { EurosPipe } from '../../shared/euros-pipe';
import { AdminOffering } from './admin-offering';

@Component({
  selector: 'app-admin-offerings-page',
  imports: [RouterLink, EurosPipe],
  templateUrl: './admin-offerings-page.html',
})
export class AdminOfferingsPage {
  protected readonly offerings = httpResource<AdminOffering[]>(() => '/api/admin/offerings', {
    defaultValue: [],
  });
}
