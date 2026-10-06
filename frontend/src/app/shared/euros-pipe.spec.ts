import { EurosPipe } from './euros-pipe';

// Intl uses non-breaking spaces before the € sign: normalise them for readable assertions.
const normalise = (value: string) => value.replace(/\s/g, ' ');

describe('EurosPipe', () => {
  const pipe = new EurosPipe();

  it('formats whole euros without decimals', () => {
    expect(normalise(pipe.transform(8000))).toBe('80 €');
  });

  it('keeps cents when needed', () => {
    expect(normalise(pipe.transform(4550))).toBe('45,50 €');
  });

  it('returns an empty string for a missing amount', () => {
    expect(pipe.transform(null)).toBe('');
  });
});
