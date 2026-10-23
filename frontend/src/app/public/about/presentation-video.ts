import { Component, ElementRef, input, signal, viewChild } from '@angular/core';
import { SiteVideo } from './about-content';

/**
 * A video of the vitrine, shown as a cover the visitor clicks to play.
 *
 * Nothing is downloaded before that click (preload="none"): the file weighs several megabytes and
 * most visitors never open it. The cover is drawn by the page rather than taken from the video,
 * so there is no poster image to keep in sync with it — and the brand shows before the first frame.
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

  private readonly player = viewChild.required<ElementRef<HTMLVideoElement>>('player');

  protected readonly playing = signal(false);
  /** The file is missing or the browser cannot read it: say so instead of leaving a black square. */
  protected readonly failed = signal(false);

  protected play(): void {
    this.playing.set(true);
    this.player().nativeElement.play().catch(() => this.failed.set(true));
  }
}
