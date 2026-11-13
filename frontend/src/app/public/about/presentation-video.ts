import { Component, ElementRef, afterNextRender, computed, input, signal, viewChild } from '@angular/core';
import { SiteVideo } from './about-content';

/**
 * How a video sits in its page:
 * - "standard": a phone-sized portrait (Séances et tarifs);
 * - "feature": large, sized by the height of the screen (two thirds on a phone, where it comes right under
 *   the title, its play button on the first screen), with a halo ("À propos");
 * - "compact": a one-line "Voir la présentation" that opens the player in place (booking page, where the
 *   calendar matters more).
 */
export type VideoVariant = 'standard' | 'feature' | 'compact';

/**
 * A video of the vitrine, shown as a cover the visitor clicks to play.
 *
 * Nothing is downloaded before that click (preload="none"): the file weighs several megabytes and
 * most visitors never open it. The cover is drawn by the page — logo, title, play button — and, when
 * the video has one, its cover image lies on top. A cover image that cannot be loaded is simply removed:
 * the drawn cover stays, never a broken image.
 *
 * The cover image is only added in the browser, once the page is live: on a page rendered by the server,
 * an image that failed before hydration would never report its error.
 *
 * The video element stays in the DOM behind the cover: play() is then called inside the click
 * itself, which is what lets the browser start it with the sound on.
 */
@Component({
  selector: 'app-presentation-video',
  templateUrl: './presentation-video.html',
})
export class PresentationVideo {
  readonly video = input.required<SiteVideo>();
  /** "none" downloads nothing before the click; "metadata" lets the browser read the duration first. */
  readonly preload = input<'none' | 'metadata'>('none');
  readonly transcriptLabel = input('Ce qui est dit dans la vidéo');
  readonly variant = input<VideoVariant>('standard');

  private readonly player = viewChild.required<ElementRef<HTMLVideoElement>>('player');

  protected readonly playing = signal(false);
  /** The file is missing or the browser cannot read it: say so instead of leaving a black square. */
  protected readonly failed = signal(false);

  private readonly live = signal(false);
  private readonly posterFailed = signal(false);
  protected readonly poster = computed(() =>
    this.live() && !this.posterFailed() ? (this.video().poster ?? null) : null,
  );

  /*
   * The classes that differ from one variant to the other, written out in full so that Tailwind finds them.
   * "feature" is sized by the height of the screen (a portrait video): its play button and title are on
   * show, the frame has a halo (night-theme.css).
   */
  protected readonly frameClass = computed(() =>
    ({
      standard: 'mx-auto block w-full max-w-[21rem]',
      feature: 'mx-auto block w-[min(100%,calc(66svh*9/16))] lg:w-[min(100%,calc(min(80vh,44rem)*9/16))]',
      compact: 'block w-full',
    })[this.variant()],
  );
  protected readonly playerClass = computed(() =>
    ({ standard: '', feature: 'video-halo', compact: 'mx-auto mt-3 max-w-[18rem]' })[this.variant()],
  );
  /**
   * Over a cover image, the title and the play button sit low on a large screen, over the darkened bottom
   * of the image; on a phone they stay in the middle, so that the play button is on the first screen.
   */
  protected readonly coverContentClass = computed(() =>
    this.poster()
      ? 'relative flex flex-col items-center gap-5 lg:mt-auto lg:pb-4'
      : 'relative flex flex-col items-center gap-5',
  );
  protected readonly titleClass = computed(() =>
    this.variant() === 'feature' ? 'font-display text-3xl sm:text-4xl' : 'font-display text-3xl',
  );
  protected readonly playDiskClass = computed(() =>
    this.variant() === 'feature'
      ? 'flex size-16 items-center justify-center rounded-full bg-gold transition-colors group-hover:bg-gold-bright'
      : 'flex size-16 items-center justify-center rounded-full border border-gold/60 transition-colors group-hover:bg-gold/10',
  );
  protected readonly playIconClass = computed(() =>
    this.variant() === 'feature' ? 'ml-1 size-7 fill-on-gold' : 'ml-1 size-7 fill-gold',
  );

  protected readonly playLabel = computed(() => {
    const { title, durationLabel } = this.video();
    return `Lire la vidéo « ${title} »${durationLabel ? `, ${durationLabel}` : ''}, avec le son`;
  });

  constructor() {
    afterNextRender(() => this.live.set(true));
  }

  /**
   * The cover gives way to the player and its controls. A browser may still refuse to start it: a click
   * made before the page was live is replayed after hydration, without the user's gesture, and the
   * sound then forbids autoplay (NotAllowedError). The controls are there: one more press plays it, so
   * this is not a failure. Only a file the browser cannot read is one, and the (error) event of the
   * video says it.
   */
  protected play(): void {
    this.playing.set(true);
    this.player()
      .nativeElement.play()
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'AbortError'))) {
          this.failed.set(true);
        }
      });
  }

  protected onPosterError(): void {
    this.posterFailed.set(true);
  }
}
