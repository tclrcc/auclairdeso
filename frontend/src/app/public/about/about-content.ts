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
  /** Shown on the cover when known ("28 secondes"). Absent: only "avec le son". */
  readonly durationLabel?: string;
  /**
   * An image of the video shown on its cover, under /contenus/ beside the video, taken from it with ffmpeg
   * (docs/exploitation/videos.md). Absent, or impossible to load: the cover drawn by the page stays.
   */
  readonly poster?: string;
  /** What is said in the video, for watching without sound and for search engines. Empty: not shown. */
  readonly transcript: string;
}

export const PRESENTATION: SiteVideo = {
  src: '/contenus/presentation.mp4',
  title: 'So se présente',
  poster: '/contenus/presentation.jpg',
  durationLabel: '28 secondes',
  transcript: '',
};

/** The title of the page, under the "À propos" eyebrow. PROVISIONAL — to validate with So. */
export const HEADLINE = 'So, en quelques mots';

/** Under the title, beside the video. */
export const INTRO: readonly string[] = [
  'So propose des séances de voyance, de magnétisme et de rééquilibrage énergétique. ' +
  'À domicile autour de Chazey-sur-Ain, en visio ou par téléphone.',
];

/** Opens "Comment se passe une demande". */
export const REQUEST_LEAD =
  'Chaque demande est lue et validée par elle, une par une. Le créneau choisi est gardé en attendant sa réponse.';

export interface RequestStep {
  readonly title: string;
  readonly text: string;
}

/**
 * "Comment se passe une demande": what the site really does, step by step (BookingPage, the e-mails of
 * the notification module). PROVISIONAL — to validate with So.
 */
export const REQUEST_STEPS: readonly RequestStep[] = [
  {
    title: 'Vous choisissez',
    text: 'Une séance, la façon de consulter qui vous convient et un créneau libre dans le calendrier.',
  },
  {
    title: 'So vous répond',
    text:
      'Elle lit votre demande et vous répond par e-mail, qu’elle l’accepte ou non. ' +
      'En attendant, le créneau vous est gardé.',
  },
  {
    title: 'La séance a lieu',
    text: 'Au moment prévu, selon le mode choisi. Vous réglez ensuite : rien n’est payé en ligne.',
  },
];

/**
 * Her path, dans ses mots : à écrire avec elle, à la première personne.
 * Tant que la liste est vide, la section n’apparaît pas — mieux vaut rien qu’un texte qu’elle n’a pas dit.
 */
export const JOURNEY: readonly string[] = [];

/**
 * Her portrait, round, for the "Faire connaissance avec So" card of the home page: a path under
 * /contenus/ (MEDIA_DIR), as the video. Null as long as there is none: the card is not shown, neither
 * is it while JOURNEY is empty, since its text is the start of her path.
 */
export const PORTRAIT: string | null = null;

/** A sound of the vitrine, under /contenus/ (MEDIA_DIR) as the videos. */
export interface SiteAudio {
  readonly src: string;
  readonly title: string;
  /** Shown under its title when known ("40 secondes"). */
  readonly durationLabel?: string;
  /** What is said, for reading instead of listening. Empty: not shown. */
  readonly transcript: string;
}

/**
 * An extract of her voice, to come: null as long as there is none, and then the "Sa voix" section is not
 * shown at all.
 */
export const VOICE: SiteAudio | null = null;

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
