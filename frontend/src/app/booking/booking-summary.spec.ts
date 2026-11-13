import { TestBed } from '@angular/core/testing';
import { BookingSummary } from './booking-summary';

describe('BookingSummary', () => {
  function open(visitorZone: string | null, start = '2026-11-16T14:00:00+01:00'): string {
    const fixture = TestBed.createComponent(BookingSummary);
    fixture.componentRef.setInput('offeringName', 'Guidance — 1 h');
    fixture.componentRef.setInput('durationMinutes', 60);
    fixture.componentRef.setInput('priceCents', 8000);
    fixture.componentRef.setInput('mode', 'PHONE');
    fixture.componentRef.setInput('start', start);
    fixture.componentRef.setInput('visitorZone', visitorZone);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent!.replace(/\s+/g, ' ');
  }

  it('gives the times of Paris, and says so', () => {
    const text = open('Europe/Paris');

    expect(text).toContain('lundi 16 novembre à 14:00');
    expect(text).toContain('heure de Paris');
    expect(text).not.toContain('chez vous');
  });

  it('adds the time of a visitor who lives in another time zone', () => {
    expect(open('America/Montreal')).toContain('heure de Paris, soit 08:00 chez vous');
  });

  it('adds the day too when it is not the same day there', () => {
    // 14:00 in Paris on 16 November is 02:00 the next day in Auckland (UTC+13 in November).
    expect(open('Pacific/Auckland')).toContain('soit le mardi 17 novembre à 02:00 chez vous');
  });

  it('adds nothing in a time zone that gives the same time as Paris', () => {
    expect(open('Europe/Brussels')).not.toContain('chez vous');
  });
});
