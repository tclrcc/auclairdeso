import { ageOn } from './admin-appointment';

describe('ageOn', () => {
  it('counts a year only once the birthday has passed', () => {
    expect(ageOn('1990-05-04', '2026-05-03')).toBe(35);
    expect(ageOn('1990-05-04', '2026-05-04')).toBe(36);
  });
});
