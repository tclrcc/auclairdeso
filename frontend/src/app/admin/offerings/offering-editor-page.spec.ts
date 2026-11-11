import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { OfferingEditorPage } from './offering-editor-page';

describe('OfferingEditorPage', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OfferingEditorPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  it('warns about health claims right above the description, and links it to the field', async () => {
    const fixture = TestBed.createComponent(OfferingEditorPage);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    const hint = page.querySelector('#description-hint');
    expect(hint?.textContent).toContain('pathologie');
    expect(hint?.textContent).toContain('aucun résultat');
    expect(hint?.textContent).toContain('cancer');
    expect(page.querySelector('#description')?.getAttribute('aria-describedby')).toBe('description-hint');
    expect(page.querySelector('label[for="description"]')?.nextElementSibling).toBe(hint);
  });

  it('no longer offers an online payment or a deposit', async () => {
    const fixture = TestBed.createComponent(OfferingEditorPage);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelector('#policy')).toBeNull();
    expect(page.querySelector('#deposit')).toBeNull();
    expect(page.textContent).not.toContain('en ligne');
  });

  it('offers two optional video fields, each explained by a hint', async () => {
    const fixture = TestBed.createComponent(OfferingEditorPage);
    await fixture.whenStable();

    const page: HTMLElement = fixture.nativeElement;
    const path = page.querySelector('#video-path');
    const transcript = page.querySelector('#video-transcript');
    expect(path?.hasAttribute('required')).toBe(false);
    expect(transcript?.hasAttribute('required')).toBe(false);
    expect(page.querySelector('#video-path-hint')?.textContent).toContain('dossier des vidéos du serveur');
    expect(page.querySelector('#video-transcript-hint')?.textContent).toContain('accessible');
    expect(path?.getAttribute('aria-describedby')).toBe('video-path-hint');
    expect(transcript?.getAttribute('aria-describedby')).toBe('video-transcript-hint');
  });
});
