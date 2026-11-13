import { TestBed } from '@angular/core/testing';
import { AvailabilityCalendar } from './availability-calendar';

describe('AvailabilityCalendar', () => {
  function open(selected: string | null = null) {
    const fixture = TestBed.createComponent(AvailabilityCalendar);
    fixture.componentRef.setInput('available', new Set(['2026-11-19', '2026-11-23', '2026-12-03']));
    fixture.componentRef.setInput('first', '2026-11-14');
    fixture.componentRef.setInput('last', '2026-12-03');
    fixture.componentRef.setInput('today', '2026-11-13');
    fixture.componentRef.setInput('month', '2026-11');
    fixture.componentRef.setInput('selected', selected);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const cell = (date: string) => element.querySelector<HTMLElement>(`[data-date="${date}"]`)!;
    return { fixture, element, cell };
  }

  it('is a grid labelled by its month, with the days of the week as column headers', () => {
    const { element } = open();

    const grid = element.querySelector('[role="grid"]')!;
    expect(element.querySelector(`#${grid.getAttribute('aria-labelledby')}`)?.textContent).toContain('novembre 2026');
    expect([...element.querySelectorAll('th .sr-only')].map((header) => header.textContent)).toEqual([
      'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche',
    ]);
  });

  it('says of each day whether it has slots, and marks today', () => {
    const { cell } = open();

    expect(cell('2026-11-19').textContent).toContain('jeudi 19 novembre, disponible');
    expect(cell('2026-11-19').hasAttribute('aria-disabled')).toBe(false);
    expect(cell('2026-11-20').textContent).toContain('vendredi 20 novembre, pas de créneau');
    expect(cell('2026-11-20').getAttribute('aria-disabled')).toBe('true');
    expect(cell('2026-11-13').getAttribute('aria-current')).toBe('date');
    expect(cell('2026-11-13').textContent).toContain("aujourd'hui, pas de créneau");
  });

  it('has a single Tab stop, on the first day with slots', () => {
    const { element, cell } = open();

    expect(element.querySelectorAll('[tabindex="0"]')).toHaveLength(1);
    expect(cell('2026-11-19').getAttribute('tabindex')).toBe('0');
  });

  it('picks a day with slots on click, and ignores a day without any', () => {
    const { fixture, cell } = open();

    cell('2026-11-20').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toBeNull();

    cell('2026-11-23').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toBe('2026-11-23');
    expect(cell('2026-11-23').getAttribute('aria-selected')).toBe('true');
    expect(cell('2026-11-23').getAttribute('tabindex')).toBe('0');
  });

  it('moves the focus with the arrows and picks the day with Enter', async () => {
    const { fixture, cell } = open();
    cell('2026-11-19').focus();

    cell('2026-11-19').dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.activeElement).toBe(cell('2026-11-26'));

    cell('2026-11-26').dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.activeElement).toBe(cell('2026-11-25'));
    // A day without slots takes the focus, so that moving stays predictable, but Enter does not pick it.
    expect(cell('2026-11-25').getAttribute('tabindex')).toBe('0');

    cell('2026-11-25').dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toBeNull();
  });

  it('turns the page with Page Down, and the month follows the focus', async () => {
    const { fixture, element, cell } = open();
    cell('2026-11-19').focus();

    cell('2026-11-19').dispatchEvent(new KeyboardEvent('keydown', { key: 'PageDown', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.month()).toBe('2026-12');
    expect(element.querySelector('h3')?.textContent).toContain('décembre 2026');
    // 19 December is past the last day the calendar reaches: the focus stops on that last day.
    expect(document.activeElement).toBe(cell('2026-12-03'));

    cell('2026-12-03').dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toBe('2026-12-03');
  });

  it('only offers the months it can reach', () => {
    const { fixture, element } = open();
    const [previous, next] = [...element.querySelectorAll<HTMLButtonElement>('button')];

    expect(previous.disabled).toBe(true);
    expect(next.disabled).toBe(false);
    expect(next.getAttribute('aria-label')).toBe('Mois suivant, décembre 2026');

    next.click();
    fixture.detectChanges();
    expect(next.disabled).toBe(true);
    expect(previous.disabled).toBe(false);
  });
});
