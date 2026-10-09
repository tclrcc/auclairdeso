import { Component, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Meta } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { formatDuration } from '../../shared/duration';
import { EurosPipe } from '../../shared/euros-pipe';
import { CATEGORY_LABELS, Offering } from '../offerings/offering';
import {
  BOOKING_STEPS,
  CATEGORY_INTROS,
  CATEGORY_ORDER,
  CONSULTATION_PLACES,
  GOOD_TO_KNOW,
} from './home-content';
import { CRESCENT_PATH, SPARKLE_PATH } from './crescent';
import { MoonPhase } from './moon-phase';
import { OpeningEye } from './opening-eye';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, EurosPipe, MoonPhase, OpeningEye],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  protected readonly offerings = httpResource<Offering[]>(() => '/api/offerings', { defaultValue: [] });

  /** The offerings by category, clairvoyance first; an empty category is not shown. */
  protected readonly groups = computed(() =>
    CATEGORY_ORDER.map((category) => ({
      category,
      label: CATEGORY_LABELS[category],
      intro: CATEGORY_INTROS[category],
      offerings: this.offerings.value().filter((offering) => offering.category === category),
    })).filter((group) => group.offerings.length > 0),
  );

  protected readonly steps = BOOKING_STEPS;
  protected readonly places = CONSULTATION_PLACES;
  protected readonly goodToKnow = GOOD_TO_KNOW;
  protected readonly crescent = CRESCENT_PATH;
  protected readonly sparkle = SPARKLE_PATH;
  protected readonly formatDuration = formatDuration;

  constructor() {
    inject(Meta).updateTag({
      name: 'description',
      content:
        'Au clair de So : voyance, magnétisme et rééquilibrage énergétique. ' +
        'Séances à domicile autour de Chazey-sur-Ain (Ain), en visio ou par téléphone. Réservation en ligne.',
    });
  }
}
