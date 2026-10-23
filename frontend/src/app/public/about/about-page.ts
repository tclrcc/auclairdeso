import { Component, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
// The sparkle is the mark of the lists of the site: the home page already draws it.
import { SPARKLE_PATH } from '../home/crescent';
import { COMMITMENTS, INTRO, JOURNEY, PRESENTATION } from './about-content';
import { PresentationVideo } from './presentation-video';

@Component({
  selector: 'app-about-page',
  imports: [RouterLink, PresentationVideo],
  templateUrl: './about-page.html',
})
export class AboutPage {
  protected readonly presentation = PRESENTATION;
  protected readonly intro = INTRO;
  protected readonly journey = JOURNEY;
  protected readonly commitments = COMMITMENTS;
  protected readonly sparkle = SPARKLE_PATH;

  constructor() {
    inject(Meta).updateTag({
      name: 'description',
      content:
        'Qui est So : sa présentation en vidéo, le cadre des séances de voyance, de magnétisme et de ' +
        'rééquilibrage énergétique, et ses engagements envers les personnes qu’elle reçoit.',
    });
  }
}
