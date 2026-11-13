import { Component, computed, input } from '@angular/core';
import { SiteVideo } from '../about/about-content';
import { PresentationVideo, VideoVariant } from '../about/presentation-video';
import { Offering } from './offering';

/** Where nginx serves MEDIA_DIR (deploy/nginx.conf); in development, frontend/public/contenus/. */
const MEDIA_URL = '/contenus/';

/**
 * The presentation video of an offering, with the player of the "À propos" page: nothing is shown when
 * the offering has no video. The transcript sits under it, in a block the visitor opens.
 *
 * Its cover image follows a naming convention rather than a field of the offering: the same name as the
 * video, in .jpg, beside it (guidance-30-min.mp4 → guidance-30-min.jpg). Without that file, the player
 * keeps its drawn cover.
 */
@Component({
  selector: 'app-offering-video',
  imports: [PresentationVideo],
  templateUrl: './offering-video.html',
})
export class OfferingVideo {
  readonly offering = input.required<Offering>();
  readonly variant = input<VideoVariant>('standard');

  protected readonly video = computed<SiteVideo | null>(() => {
    const { videoPath, videoTranscript, name } = this.offering();
    return videoPath
      ? {
          src: MEDIA_URL + encodeURIComponent(videoPath),
          poster: MEDIA_URL + encodeURIComponent(videoPath.replace(/\.(mp4|webm)$/, '.jpg')),
          title: name,
          transcript: videoTranscript ?? '',
        }
      : null;
  });
}
