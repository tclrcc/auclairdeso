import { formatDuration } from './duration';

describe('formatDuration', () => {
  it('writes durations the French way', () => {
    expect(formatDuration(30)).toBe('30 min');
    expect(formatDuration(60)).toBe('1 h');
    expect(formatDuration(90)).toBe('1 h 30');
    expect(formatDuration(65)).toBe('1 h 05');
  });
});
