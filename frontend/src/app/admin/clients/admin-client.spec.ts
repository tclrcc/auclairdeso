import { clientsUrl, formatDateTime, toCorrection } from './admin-client';

describe('clientsUrl', () => {
  it('lists every client of the filter when the search is empty', () => {
    expect(clientsUrl('  ', 'BLOCKED')).toBe('/api/admin/clients?query=&filter=BLOCKED');
  });

  it('waits for a second character', () => {
    expect(clientsUrl('m', 'ALL')).toBeUndefined();
    expect(clientsUrl('ma', 'ALL')).toBe('/api/admin/clients?query=ma&filter=ALL');
  });

  it('encodes what is typed', () => {
    expect(clientsUrl('+33 6', 'ALL')).toBe('/api/admin/clients?query=%2B33%206&filter=ALL');
  });
});

describe('toCorrection', () => {
  it('trims the text and sends nothing for an empty optional field', () => {
    expect(
      toCorrection({
        firstName: ' Alice ',
        lastName: 'Martin',
        birthDate: '',
        phone: '06 12 34 56 78 ',
        city: '  ',
        email: '',
      }),
    ).toEqual({
      firstName: 'Alice',
      lastName: 'Martin',
      birthDate: null,
      phone: '06 12 34 56 78',
      city: null,
      email: null,
    });
  });
});

describe('formatDateTime', () => {
  it('writes the date with its year, in Paris time', () => {
    expect(formatDateTime('2026-10-12T12:05:00Z')).toBe('12 octobre 2026 à 14:05');
  });
});
