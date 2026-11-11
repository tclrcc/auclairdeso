import { Component, computed, input } from '@angular/core';
import { SiteVideo } from '../about/about-content';
import { PresentationVideo } from '../about/presentation-video';
import { Offering } from './offering';

/** Where nginx serves MEDIA_DIR (deploy/nginx.conf); in development, frontend/public/contenus/. */
const MEDIA_URL = '/contenus/';

/**
 * The presentation video of an offering, with the player of the "À propos" page: nothing is shown when
 * the offering has no video. The transcript sits under it, in a block the visitor opens.
 */
@Component({
  selector: 'app-offering-video',
  imports: [PresentationVideo],
  templateUrl: './offering-video.html',
})
export class OfferingVideo {
  readonly offering = input.required<Offering>();

  protected readonly video = computed<SiteVideo | null>(() => {
    const { videoPath, videoTranscript, name } = this.offering();
    return videoPath
      ? { src: MEDIA_URL + encodeURIComponent(videoPath), title: name, transcript: videoTranscript ?? '' }
      : null;
  });
}
