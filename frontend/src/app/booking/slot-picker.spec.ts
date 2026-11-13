import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeSlot } from './booking';
import { SlotPicker } from './slot-picker';

/** Friday 13 November 2026, 11:00 in Paris: the first bookable day is Saturday 14. */
const NOW = new Date('2026-11-13T10:00:00Z');
const URL_OF_THE_HORIZON = '/api/offerings/guidance-1-h/slots?from=2026-11-14&to=2027-01-15';

const SLOTS: TimeSlot[] = [
  { start: '2026-11-19T14:00:00+01:00', end: '2026-11-19T15:00:00+01:00' },
  { start: '2026-11-19T15:15:00+01:00', end: '2026-11-19T16:15:00+01:00' },
  { start: '2026-11-19T16:30:00+01:00', end: '2026-11-19T17:30:00+01:00' },
  { start: '2026-11-23T14:00:00+01:00', end: '2026-11-23T15:00:00+01:00' },
  { start: '2026-12-03T14:00:00+01:00', end: '2026-12-03T15:00:00+01:00' },
];

describe('SlotPicker', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(NOW);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  async function open(slots: TimeSlot[] | 'error' = SLOTS, selected: string | null = null) {
    const fixture = TestBed.createComponent(SlotPicker);
    fixture.componentRef.setInput('slug', 'guidance-1-h');
    fixture.componentRef.setInput('selected', selected);
    fixture.detectChanges();
    await respond(fixture, slots);
    const element = fixture.nativeElement as HTMLElement;
    return { fixture, element };
  }

  async function respond(fixture: { detectChanges(): void; whenStable(): Promise<unknown> }, slots: TimeSlot[] | 'error') {
    const request = httpTesting.expectOne(URL_OF_THE_HORIZON);
    if (slots === 'error') {
      request.flush(null, { status: 503, statusText: 'Service Unavailable' });
    } else {
      request.flush(slots);
    }
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function times(element: HTMLElement): string[] {
    return [...element.querySelectorAll('.slot-chip')].map((chip) => chip.textContent!.trim());
  }

  function button(element: HTMLElement, text: string): HTMLButtonElement {
    return [...element.querySelectorAll('button')].find((candidate) => candidate.textContent?.includes(text))!;
  }

  it('asks for the whole horizon at once, from tomorrow, and shows the month of the first slot', async () => {
    // expectOne in open() fails if the URL differs or if a second request is sent.
    const { element } = await open();

    expect(element.querySelector('h3')?.textContent).toContain('novembre 2026');
    expect(element.querySelector('[data-date="2026-11-19"]')?.classList).toContain('calendar-day-available');
    expect(element.textContent).toContain('Choisissez un jour pour voir ses horaires.');
  });

  it('says it is looking while the slots are on their way', () => {
    const fixture = TestBed.createComponent(SlotPicker);
    fixture.componentRef.setInput('slug', 'guidance-1-h');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Recherche des créneaux…');
    httpTesting.expectOne(URL_OF_THE_HORIZON).flush([]);
  });

  it('jumps to the first available day and offers its times as radio buttons', async () => {
    const { fixture, element } = await open();

    button(element, 'Première disponibilité').click();
    fixture.detectChanges();

    expect(element.querySelector('legend')?.textContent).toContain('jeudi 19 novembre');
    expect(element.querySelector('legend')?.textContent).toContain('heure de Paris');
    expect(times(element)).toEqual(['14:00', '15:15', '16:30']);
    expect(element.querySelectorAll('input[type="radio"][name="slot"]')).toHaveLength(3);
    // Once there, the shortcut has nothing left to offer.
    expect(button(element, 'Première disponibilité')).toBeUndefined();
  });

  it('shares the picked time with the page', async () => {
    const { fixture, element } = await open();
    element.querySelector<HTMLElement>('[data-date="2026-11-19"]')!.click();
    fixture.detectChanges();

    const radios = element.querySelectorAll<HTMLInputElement>('input[name="slot"]');
    radios[1].click();
    fixture.detectChanges();

    expect(fixture.componentInstance.selected()).toBe('2026-11-19T15:15:00+01:00');
    expect(radios[1].checked).toBe(true);
  });

  it('forgets the picked time when the client turns to another day', async () => {
    const { fixture, element } = await open(SLOTS, '2026-11-19T15:15:00+01:00');

    element.querySelector<HTMLElement>('[data-date="2026-11-23"]')!.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.selected()).toBeNull();
    expect(times(element)).toEqual(['14:00']);
  });

  it('opens on the slot kept through the login, with its time already checked', async () => {
    const { fixture, element } = await open(SLOTS, '2026-12-03T14:00:00+01:00');

    expect(element.querySelector('h3')?.textContent).toContain('décembre 2026');
    expect(element.querySelector('input[name="slot"]:checked')?.parentElement?.textContent).toContain('14:00');
    expect(fixture.componentInstance.selected()).toBe('2026-12-03T14:00:00+01:00');
  });

  it('says so when the slot kept through the login has been taken meanwhile', async () => {
    const { fixture, element } = await open(SLOTS, '2026-11-19T17:45:00+01:00');

    expect(fixture.componentInstance.selected()).toBeNull();
    expect(element.querySelector('[role="status"]')?.textContent).toContain("n'est plus disponible");
    // The day stays open, with what is left on it.
    expect(times(element)).toEqual(['14:00', '15:15', '16:30']);
  });

  it('keeps the day open when asked again after the chosen slot was taken', async () => {
    const { fixture, element } = await open(SLOTS, '2026-11-23T14:00:00+01:00');

    fixture.componentRef.setInput('selected', null);
    fixture.componentInstance.reload();
    fixture.detectChanges();
    await respond(fixture, SLOTS.filter((slot) => !slot.start.startsWith('2026-11-23')));

    expect(element.querySelector('legend')?.textContent).toContain('lundi 23 novembre');
    expect(element.textContent).toContain('Plus aucun horaire libre ce jour-là.');
  });

  it('says plainly that nothing is free, without naming a length of horizon', async () => {
    const { element } = await open([]);

    expect(element.querySelector('[role="grid"]')).toBeNull();
    expect(element.textContent).toContain("Aucun créneau n'est libre pour cette séance en ce moment.");
    expect(element.textContent).not.toMatch(/mois|semaine/);
  });

  it('offers to try again when the slots cannot be loaded', async () => {
    const { fixture, element } = await open('error');

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('ne peuvent pas être affichés');

    button(element, 'Réessayer').click();
    fixture.detectChanges();
    await respond(fixture, SLOTS);

    expect(element.querySelector('[role="grid"]')).not.toBeNull();
  });
});
