import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ComingSoon } from './coming-soon';

describe('ComingSoon', () => {
  let fixture: ComponentFixture<ComingSoon>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComingSoon],
    }).compileComponents();

    fixture = TestBed.createComponent(ComingSoon);
    await fixture.whenStable();
  });

  it('should display the brand name', () => {
    const heading: HTMLElement = fixture.nativeElement.querySelector('h1');
    expect(heading.textContent).toContain('Au clair de So');
  });

  it('should open both social links in a new tab', () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('nav a'));
    expect(links).toHaveLength(2);
    links.forEach((link) => expect(link.target).toBe('_blank'));
  });
});
