import { TestBed } from '@angular/core/testing';
import { Offering } from './offering';
import { OfferingVideo } from './offering-video';

const GUIDANCE: Offering = {
  slug: 'guidance-30-min',
  name: 'Guidance — 30 min',
  description: 'Une séance courte.',
  durationMinutes: 30,
  bufferMinutes: 30,
  priceCents: 4500,
  paymentPolicy: 'ON_SITE',
  depositCents: null,
  modes: ['VIDEO'],
  category: 'CLAIRVOYANCE',
  videoPath: null,
  videoTranscript: null,
};

describe('OfferingVideo', () => {
  function open(offering: Offering): HTMLElement {
    const fixture = TestBed.createComponent(OfferingVideo);
    fixture.componentRef.setInput('offering', offering);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('draws nothing for an offering without a video', () => {
    const element = open(GUIDANCE);

    expect(element.querySelector('video')).toBeNull();
    expect(element.querySelector('details')).toBeNull();
    expect(element.textContent?.trim()).toBe('');
  });

  it('plays the file served under /contenus/, with the controls and without autoplay', () => {
    const video = open({ ...GUIDANCE, videoPath: 'guidance-30-min.mp4' }).querySelector('video');

    expect(video?.getAttribute('src')).toBe('/contenus/guidance-30-min.mp4');
    expect(video?.getAttribute('preload')).toBe('metadata');
    expect(video?.hasAttribute('controls')).toBe(true);
    expect(video?.hasAttribute('autoplay')).toBe(false);
  });

  it('puts the transcript under the video, in a closed block titled "Lire la transcription"', () => {
    const element = open({
      ...GUIDANCE,
      videoPath: 'guidance-30-min.mp4',
      videoTranscript: 'Bonjour, je suis So.',
    });

    const details = element.querySelector('details');
    expect(details?.querySelector('summary')?.textContent).toContain('Lire la transcription');
    expect(details?.textContent).toContain('Bonjour, je suis So.');
    expect(details?.open).toBe(false);
    expect(element.querySelector('video')!.compareDocumentPosition(details!)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('has no transcript block while no transcript is written', () => {
    expect(open({ ...GUIDANCE, videoPath: 'guidance-30-min.mp4' }).querySelector('details')).toBeNull();
  });
});
