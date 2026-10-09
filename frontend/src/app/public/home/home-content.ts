import { OfferingCategory } from '../offerings/offering';

/*
 * The texts of the home page, gathered in one place to be reviewed with So.
 * Rule for every text: describe the session, never promise a result.
 */

/** The order of the categories on the page: clairvoyance first. */
export const CATEGORY_ORDER: readonly OfferingCategory[] = ['CLAIRVOYANCE', 'MAGNETISM', 'ENERGY_REBALANCING'];

export const CATEGORY_INTROS: Record<OfferingCategory, string> = {
  CLAIRVOYANCE:
    'Un échange autour de ce qui vous occupe : vie sentimentale, famille, travail, un choix à faire. ' +
    'La séance part de votre question et de votre photo.',
  MAGNETISM:
    'Un accompagnement par le magnétisme, en présence ou à distance. ' +
    'Il vient en complément d’un suivi médical, jamais à sa place.',
  ENERGY_REBALANCING: 'Un temps calme pour faire le point sur votre énergie, en présence ou à distance.',
};

export interface BookingStep {
  /** The lit part of the moon drawn next to the step, from 0 (new moon) to 1 (full moon). */
  readonly illuminated: number;
  readonly title: string;
  readonly text: string;
}

export const BOOKING_STEPS: readonly BookingStep[] = [
  {
    illuminated: 0.15,
    title: 'Choisissez votre séance',
    text: 'Puis la façon de consulter et un créneau libre, du lendemain jusqu’à deux mois à l’avance.',
  },
  {
    illuminated: 0.5,
    title: 'Confirmez votre adresse email',
    text: 'Vous recevez un lien qui vous ramène à votre demande. Pas de compte à créer, pas de mot de passe.',
  },
  {
    illuminated: 0.8,
    title: 'So valide votre demande',
    text: 'Chaque demande est lue personnellement. Le créneau vous est réservé en attendant sa réponse par email.',
  },
  {
    illuminated: 1,
    title: 'La séance',
    text: 'Vous réglez après la séance, par virement ou en espèces.',
  },
];

export interface PracticalInfo {
  readonly title: string;
  readonly text: string;
}

export const CONSULTATION_PLACES: readonly PracticalInfo[] = [
  { title: 'À domicile', text: 'Chez vous, dans un rayon de 20 km autour de Chazey-sur-Ain.' },
  { title: 'En visio', text: 'Par Messenger, à l’heure du rendez-vous.' },
  { title: 'Par téléphone', text: 'Vous appelez le numéro indiqué dans l’email de confirmation.' },
  { title: 'En présentiel', text: 'Réservé aux personnes déjà suivies.' },
];

export const GOOD_TO_KNOW: readonly string[] = [
  'Le prix est le même, quelle que soit la façon de consulter.',
  'Rien n’est payé en ligne : vous réglez après la séance.',
  'Vous pouvez annuler librement jusqu’à 48 h avant la séance.',
  'Les séances sont réservées aux personnes majeures.',
];
