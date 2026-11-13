import { Component, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
// The four-pointed sparkle of the logo marks the lists of the site.
import { SPARKLE_PATH } from '../home/sparkle';
import { COMMITMENTS, HEADLINE, INTRO, JOURNEY, PORTRAIT, PRESENTATION, REQUEST_LEAD, REQUEST_STEPS, VOICE } from './about-content';
import { PresentationVideo } from './presentation-video';
import { VoiceExtract } from './voice-extract';

@Component({
  selector: 'app-about-page',
  imports: [NgOptimizedImage, RouterLink, PresentationVideo, VoiceExtract],
  templateUrl: './about-page.html',
})
export class AboutPage {
  protected readonly presentation = PRESENTATION;
  protected readonly headline = HEADLINE;
  protected readonly intro = INTRO;
  protected readonly requestLead = REQUEST_LEAD;
  protected readonly requestSteps = REQUEST_STEPS;
  protected readonly journey = JOURNEY;
  /** Her round portrait, under /contenus/; null while there is none, and then no image at all. */
  protected readonly portrait = PORTRAIT;
  /** An extract of her voice; null while there is none, and then no section at all. */
  protected readonly voice = VOICE;
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
