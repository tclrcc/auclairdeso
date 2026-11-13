import { Component, input } from '@angular/core';
import { SiteAudio } from './about-content';

/**
 * An extract of her voice: the native player, nothing downloaded before the visitor presses play
 * (preload="none"), never started on its own. Its transcript sits under it, in a block the visitor opens,
 * as for the videos.
 */
@Component({
  selector: 'app-voice-extract',
  template: `
    <figure class="night-card p-6 md:p-8">
      <figcaption>
        <span class="night-card-title block">{{ audio().title }}</span>
        @if (audio().durationLabel; as duration) {
          <span class="mt-1 block text-sm text-ink-muted">{{ duration }}</span>
        }
      </figcaption>
      <audio class="mt-5 w-full" [src]="audio().src" preload="none" controls></audio>
      @if (audio().transcript) {
        <details class="mt-4">
          <summary class="cursor-pointer text-sm text-gold underline underline-offset-4 hover:text-gold-bright">
            Lire la transcription
          </summary>
          <p class="mt-2 whitespace-pre-line text-sm leading-relaxed text-ink-soft">{{ audio().transcript }}</p>
        </details>
      }
    </figure>
  `,
})
export class VoiceExtract {
  readonly audio = input.required<SiteAudio>();
}
