import { Component, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
// The four-pointed sparkle of the logo marks the lists of the site.
import { SPARKLE_PATH } from '../home/sparkle';
import { COMMITMENTS, INTRO, JOURNEY, PORTRAIT, PRESENTATION } from './about-content';
import { PresentationVideo } from './presentation-video';

@Component({
  selector: 'app-about-page',
  imports: [NgOptimizedImage, RouterLink, PresentationVideo],
  templateUrl: './about-page.html',
})
export class AboutPage {
  protected readonly presentation = PRESENTATION;
  protected readonly intro = INTRO;
  protected readonly journey = JOURNEY;
  /** Her round portrait, under /contenus/; null while there is none, and then no image at all. */
  protected readonly portrait = PORTRAIT;
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
