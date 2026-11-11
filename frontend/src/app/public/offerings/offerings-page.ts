import { Component, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Meta } from '@angular/platform-browser';
import { EurosPipe } from '../../shared/euros-pipe';
import { ConsultationMode, MODE_LABELS, Offering } from './offering';
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

  /**
   * The list is served to anyone, and the practitioner's own premises are reserved to clients she
   * has marked as trusted (spec 3.4): the booking page shows that mode to them, this one never does.
   */
  protected publicModesOf(offering: Offering): ConsultationMode[] {
    return offering.modes.filter((mode) => mode !== 'IN_PERSON');
  }

  constructor() {
    inject(Meta).updateTag({
      name: 'description',
      content: 'Séances proposées par Au clair de So : durée, modes de consultation et tarifs.',
    });
  }
}
