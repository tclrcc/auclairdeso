import { Component, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Meta } from '@angular/platform-browser';
import { EurosPipe } from '../../shared/euros-pipe';
import { MODE_LABELS, Offering } from './offering';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-offerings-page',
  imports: [EurosPipe, RouterLink],
  templateUrl: './offerings-page.html',
})
export class OfferingsPage {
  protected readonly offerings = httpResource<Offering[]>(() => '/api/offerings', {
    defaultValue: [],
  });

  protected readonly modeLabels = MODE_LABELS;

  constructor() {
    inject(Meta).updateTag({
      name: 'description',
      content: 'Séances proposées par Au clair de So : durée, modes de consultation et tarifs.',
    });
  }
}
