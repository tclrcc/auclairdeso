import { OfferingCategory } from '../offerings/offering';

/*
 * The texts of the home page, gathered in one place to be reviewed with So.
 * Rule for every text: describe the session, never promise a result.
 */

export const HERO = {
  eyebrow: 'Voyance · Magnétisme · Rééquilibrage énergétique',
  title: 'Une présence à l’écoute, pour y voir plus clair',
  text:
    'Des séances sur rendez-vous, autour de Chazey-sur-Ain et à distance. ' +
    'Choisissez votre séance, demandez un créneau, So vous confirme.',
  offeringsLink: 'Voir les séances',
  aboutLink: 'Découvrir So',
};

/** The order of the offerings on the page: clairvoyance first. */
export const CATEGORY_ORDER: readonly OfferingCategory[] = ['CLAIRVOYANCE', 'MAGNETISM', 'ENERGY_REBALANCING'];

export interface BookingStep {
  readonly title: string;
  readonly text: string;
}

export const BOOKING_STEPS: readonly BookingStep[] = [
  {
    title: 'Choisissez votre séance',
    text: 'Chaque séance indique son format, sa durée et son tarif.',
  },
  {
    title: 'Demandez un créneau',
    text: 'Chaque demande est validée à la main par So, qui garde la main sur son agenda.',
  },
  {
    title: 'Recevez la confirmation',
    text:
      'Un e-mail vous prévient. Le règlement se fait après la séance : virement, espèces ou paiement mobile.',
  },
];

/** The card that introduces So, shown only once her portrait and her path are written (about-content.ts). */
export const ABOUT_PREVIEW = {
  title: 'Faire connaissance avec So',
  link: 'Découvrir So',
};
