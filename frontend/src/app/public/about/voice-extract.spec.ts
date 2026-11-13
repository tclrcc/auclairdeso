import { TestBed } from '@angular/core/testing';
import { SiteAudio } from './about-content';
import { VoiceExtract } from './voice-extract';

const EXTRACT: SiteAudio = {
  src: '/contenus/voix-de-test.mp3',
  title: 'Sa voix',
  durationLabel: '40 secondes',
  transcript: '',
};

describe('VoiceExtract', () => {
  function open(audio: SiteAudio): HTMLElement {
    const fixture = TestBed.createComponent(VoiceExtract);
    fixture.componentRef.setInput('audio', audio);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('downloads nothing before the visitor presses play, and never starts on its own', () => {
    const audio = open(EXTRACT).querySelector('audio')!;

    expect(audio.getAttribute('src')).toBe('/contenus/voix-de-test.mp3');
    expect(audio.getAttribute('preload')).toBe('none');
    expect(audio.hasAttribute('controls')).toBe(true);
    expect(audio.hasAttribute('autoplay')).toBe(false);
  });

  it('puts what is said under the player once it is written down', () => {
    expect(open(EXTRACT).querySelector('details')).toBeNull();

    const element = open({ ...EXTRACT, transcript: 'Bonjour.' });
    const details = element.querySelector('details')!;
    expect(details.textContent).toContain('Bonjour.');
    expect(element.querySelector('audio')!.compareDocumentPosition(details)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });
});
