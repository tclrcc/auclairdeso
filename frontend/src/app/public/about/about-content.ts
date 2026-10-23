/*
 * The texts of the "À propos" page, gathered in one place to be reviewed with So, as home-content.ts is.
 * Rule for every text: describe the session, never promise a result, no medical, legal or financial advice.
 *
 * An empty list or an empty text hides its section: a visitor never sees a placeholder.
 */

export interface SiteVideo {
  /**
   * Served by the nginx of the VPS from MEDIA_DIR (deploy/nginx.conf), and in development from
   * frontend/public/contenus/. The file is never committed: the repository holds code, not media.
   * "/contenus" and not "/media", which the Angular build already uses for the fonts.
   */
  readonly src: string;
  readonly title: string;
  readonly durationLabel: string;
  /** What is said in the video, for watching without sound and for search engines. Empty: not shown. */
  readonly transcript: string;
}

export const PRESENTATION: SiteVideo = {
  src: '/contenus/presentation.mp4',
  title: 'So se présente',
  durationLabel: '28 secondes',
  transcript: '',
};

export const INTRO: readonly string[] = [
  'So propose des séances de voyance, de magnétisme et de rééquilibrage énergétique. ' +
  'À domicile autour de Chazey-sur-Ain, en visio ou par téléphone.',
  'Chaque demande est lue et validée par elle, une par une. Le créneau choisi est gardé en attendant sa réponse.',
];

/**
 * Her path, dans ses mots : à écrire avec elle, à la première personne.
 * Tant que la liste est vide, la section n’apparaît pas — mieux vaut rien qu’un texte qu’elle n’a pas dit.
 */
export const JOURNEY: readonly string[] = [];

export interface Commitment {
  readonly title: string;
  readonly text: string;
}

/** Each one is what the application really does: it can be checked, not just promised. */
export const COMMITMENTS: readonly Commitment[] = [
  {
    title: 'Aucune promesse de résultat',
    text: 'Une séance est un accompagnement : elle ne garantit rien et ne vous engage à rien pour la suite.',
  },
  {
    title: 'Ni médecine, ni droit, ni finance',
    text:
      'Les séances ne remplacent ni un avis ou un traitement médical, ni un conseil juridique ou financier. ' +
      'Dans ces domaines, c’est vers un professionnel qu’il faut se tourner.',
  },
  {
    title: 'Réservé aux personnes majeures',
    text: 'Les séances s’adressent aux personnes majeures. C’est rappelé au moment de la réservation.',
  },
  {
    title: 'Vous annulez librement',
    text: 'Jusqu’à 48 heures avant la séance, annuler ne coûte rien et ne demande aucune explication.',
  },
  {
    title: 'Vous réglez après la séance',
    text:
      'Rien n’est payé en ligne. Le règlement se fait une fois la séance rendue, par virement, ' +
      'en espèces ou par paiement mobile, et une note vous est remise.',
  },
  {
    title: 'Votre photo est effacée',
    text: 'La photo que vous joignez à votre demande est supprimée sept jours après la séance, automatiquement.',
  },
  {
    title: 'Votre compte vous appartient',
    text:
      'Depuis « Mes rendez-vous », vous le supprimez quand vous le souhaitez. ' +
      'Ce qui reste ensuite dans les comptes de l’entreprise ne vous nomme plus.',
  },
];
