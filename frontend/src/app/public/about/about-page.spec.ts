import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AboutPage } from './about-page';
import { SiteVideo } from './about-content';
import { PresentationVideo, VideoVariant } from './presentation-video';

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

  it('opens on her presentation video, large, right after the title', () => {
    const page = openPage().nativeElement as HTMLElement;

    const title = page.querySelector('h1')!;
    const video = page.querySelector('video')!;
    expect(title.textContent).toContain('So, en quelques mots');
    expect(title.compareDocumentPosition(video)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(video.closest('.video-halo')).not.toBeNull();
    expect(video.hasAttribute('autoplay')).toBe(false);
  });

  it('explains how a request goes, in three steps, without naming any length of horizon', () => {
    const page = openPage().nativeElement as HTMLElement;

    const steps = page.querySelectorAll('section[aria-labelledby="request-title"] ol > li');
    expect(steps).toHaveLength(3);
    expect(steps[1].textContent).toContain('vous répond par e-mail, qu’elle l’accepte ou non');
    expect(page.textContent).not.toMatch(/deux mois|lendemain/);
  });

  it('keeps her own words hidden until she has said them', () => {
    // JOURNEY is empty: better no section at all than a text written in her place.
    expect(openPage().nativeElement.textContent).not.toContain('Son parcours');
  });

  it('shows no placeholder for what is still missing: no portrait, no text in brackets', () => {
    const page = openPage().nativeElement as HTMLElement;

    // PORTRAIT is null: no image of her at all (the logo of the video cover is not a portrait).
    expect(page.querySelector('img[alt="Portrait de So"]')).toBeNull();
    expect(page.textContent).not.toMatch(/[[\]]/);
  });

  describe('presentation video', () => {
    function openVideo(video: SiteVideo = VIDEO, variant: VideoVariant = 'standard') {
      const fixture = TestBed.createComponent(PresentationVideo);
      fixture.componentRef.setInput('video', video);
      fixture.componentRef.setInput('variant', variant);
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

    it('leaves the controls to the visitor when the browser refuses to start the video', async () => {
      vi.spyOn(HTMLMediaElement.prototype, 'play').mockRejectedValue(new DOMException('No gesture', 'NotAllowedError'));
      const fixture = openVideo();
      const element = fixture.nativeElement as HTMLElement;

      element.querySelector('button')!.click();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(element.querySelector('button')).toBeNull();
      expect(element.querySelector('video')?.hasAttribute('controls')).toBe(true);
      expect(element.querySelector('[role="alert"]')).toBeNull();
    });

    it('says so instead of leaving a black square when the file is missing', () => {
      const fixture = openVideo();
      const element = fixture.nativeElement as HTMLElement;

      element.querySelector('video')!.dispatchEvent(new Event('error'));
      fixture.detectChanges();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('ne peut pas être lue');
    });

    it('names the play button after the video, its duration and its sound', () => {
      const button = openVideo().nativeElement.querySelector('button') as HTMLButtonElement;

      expect(button.getAttribute('aria-label')).toBe('Lire la vidéo « So se présente », 28 secondes, avec le son');
    });

    it('lays the cover image on the drawn cover once the page is live, and removes it if it cannot load', async () => {
      const fixture = openVideo({ ...VIDEO, poster: '/contenus/presentation-de-test.jpg' });
      await fixture.whenStable();
      fixture.detectChanges();
      const element = fixture.nativeElement as HTMLElement;

      const poster = element.querySelector<HTMLImageElement>('img[src="/contenus/presentation-de-test.jpg"]')!;
      expect(poster.getAttribute('alt')).toBe('');
      expect(element.querySelector('img[src="logo.webp"]')).toBeNull();

      poster.dispatchEvent(new Event('error'));
      fixture.detectChanges();

      expect(element.querySelector('img[src="/contenus/presentation-de-test.jpg"]')).toBeNull();
      expect(element.querySelector('img[src="logo.webp"]')).not.toBeNull();
      expect(element.querySelector('[role="alert"]')).toBeNull();
    });

    it('in its compact form, is one line that opens the player in place', () => {
      const play = vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue();
      const fixture = openVideo(VIDEO, 'compact');
      const element = fixture.nativeElement as HTMLElement;

      expect(element.querySelector('button')?.textContent).toContain('Voir la présentation');
      expect(element.querySelector('video')?.closest('[hidden]')).not.toBeNull();

      element.querySelector('button')!.click();
      fixture.detectChanges();

      expect(play).toHaveBeenCalledOnce();
      expect(element.querySelector('button')).toBeNull();
      expect(element.querySelector('video')?.closest('[hidden]')).toBeNull();
    });

    it('offers what is said in the video once it has been written down', () => {
      expect((openVideo().nativeElement as HTMLElement).querySelector('details')).toBeNull();

      const fixture = openVideo({ ...VIDEO, transcript: 'Bonjour, je suis So.' });

      expect((fixture.nativeElement as HTMLElement).querySelector('details')?.textContent)
        .toContain('Bonjour, je suis So.');
    });
  });
});
