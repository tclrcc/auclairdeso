import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AboutPage } from './about-page';
import { SiteVideo } from './about-content';
import { PresentationVideo } from './presentation-video';

const VIDEO: SiteVideo = {
  src: '/contenus/presentation-de-test.mp4',
  title: 'So se présente',
  durationLabel: '28 secondes',
  transcript: '',
};

describe('about page', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  afterEach(() => vi.restoreAllMocks());

  function openPage() {
    const fixture = TestBed.createComponent(AboutPage);
    fixture.detectChanges();
    return fixture;
  }

  it('states the frame of the sessions rather than promising a result', () => {
    const text = (openPage().nativeElement.textContent as string).replace(/\s+/g, ' ');

    expect(text).toContain('Aucune promesse de résultat');
    expect(text).toContain('ni un avis ou un traitement médical');
    expect(text).toContain('personnes majeures');
  });

  it('keeps her own words hidden until she has said them', () => {
    // JOURNEY is empty: better no section at all than a text written in her place.
    expect(openPage().nativeElement.textContent).not.toContain('Son parcours');
  });

  describe('presentation video', () => {
    function openVideo(video: SiteVideo = VIDEO) {
      const fixture = TestBed.createComponent(PresentationVideo);
      fixture.componentRef.setInput('video', video);
      fixture.detectChanges();
      return fixture;
    }

    it('downloads nothing before the visitor asks for it', () => {
      const element = openVideo().nativeElement as HTMLElement;

      expect(element.querySelector('video')?.getAttribute('preload')).toBe('none');
      expect(element.querySelector('button')?.textContent).toContain('28 secondes');
    });

    it('starts the video from the click itself, which is what allows the sound', () => {
      const play = vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue();
      const fixture = openVideo();

      (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
      fixture.detectChanges();

      expect(play).toHaveBeenCalledOnce();
      expect((fixture.nativeElement as HTMLElement).querySelector('button')).toBeNull();
    });

    it('says so instead of leaving a black square when the file is missing', () => {
      const fixture = openVideo();
      const element = fixture.nativeElement as HTMLElement;

      element.querySelector('video')!.dispatchEvent(new Event('error'));
      fixture.detectChanges();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('ne peut pas être lue');
    });

    it('offers what is said in the video once it has been written down', () => {
      expect((openVideo().nativeElement as HTMLElement).querySelector('details')).toBeNull();

      const fixture = openVideo({ ...VIDEO, transcript: 'Bonjour, je suis So.' });

      expect((fixture.nativeElement as HTMLElement).querySelector('details')?.textContent)
        .toContain('Bonjour, je suis So.');
    });
  });
});
