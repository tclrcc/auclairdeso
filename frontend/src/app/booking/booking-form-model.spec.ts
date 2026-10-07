import { emptyBookingForm, isAdult, toBookingRequest } from './booking-form-model';

describe('booking form model', () => {
  it('counts majority from the eighteenth birthday', () => {
    expect(isAdult('2008-10-11', '2026-10-11')).toBe(true);
    expect(isAdult('2008-10-12', '2026-10-11')).toBe(false);
  });

  it('sends only the details the chosen mode needs', () => {
    const request = toBookingRequest('guidance-1-h', 'PHONE', '2026-10-12T14:00:00+02:00', {
      ...emptyBookingForm(),
      firstName: ' Alice ',
      city: '   ',
      address: '1 rue des Lilas',
      messengerName: 'Alice M',
    });

    expect(request.client.firstName).toBe('Alice');
    expect(request.client.city).toBeNull();
    expect(request.address).toBeNull();
    expect(request.messengerName).toBeNull();
  });

  it('keeps the address for a session at home', () => {
    const request = toBookingRequest('guidance-1-h', 'CLIENT_HOME', '2026-10-12T14:00:00+02:00', {
      ...emptyBookingForm(),
      address: ' 1 rue des Lilas ',
    });

    expect(request.address).toBe('1 rue des Lilas');
  });
});
