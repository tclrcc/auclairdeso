import { Component, computed, inject } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Meta } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { formatDuration } from '../../shared/duration';
import { EurosPipe } from '../../shared/euros-pipe';
import { JOURNEY, PORTRAIT } from '../about/about-content';
import { CATEGORY_LABELS, Offering } from '../offerings/offering';
import { ABOUT_PREVIEW, BOOKING_STEPS, CATEGORY_ORDER, HERO } from './home-content';
import { HeroIllustration } from './hero-illustration';

/** What the "Faire connaissance avec So" card needs; null hides it. */
export interface AboutPreview {
  readonly portrait: string;
  readonly excerpt: string;
}

/** The card shows real content only: no portrait or no written path, no card. */
export function aboutPreviewOf(portrait: string | null, journey: readonly string[]): AboutPreview | null {
  return portrait && journey.length > 0 ? { portrait, excerpt: journey[0] } : null;
}

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, NgOptimizedImage, EurosPipe, HeroIllustration],
  templateUrl: './home-page.html',
})
export class HomePage {
  protected readonly offerings = httpResource<Offering[]>(() => '/api/offerings', { defaultValue: [] });

  /** Clairvoyance first, then magnetism, then energy rebalancing; the order of the API within each. */
  protected readonly sortedOfferings = computed(() =>
    [...this.offerings.value()].sort(
      (a, b) => CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category),
    ),
  );

  protected readonly hero = HERO;
  protected readonly steps = BOOKING_STEPS;
  protected readonly about = ABOUT_PREVIEW;
  protected readonly aboutPreview = aboutPreviewOf(PORTRAIT, JOURNEY);
  protected readonly categoryLabels = CATEGORY_LABELS;
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
