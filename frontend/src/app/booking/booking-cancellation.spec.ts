import { AppointmentView } from './booking';
import { canCancel, cancellationQuestion } from './booking-cancellation';

const CONFIRMED: AppointmentView = {
  id: 1,
  offeringSlug: 'guidance-1-h',
  offeringName: 'Guidance — 1 h',
  mode: 'VIDEO',
  start: '2026-10-12T14:00:00+02:00',
  end: '2026-10-12T15:00:00+02:00',
  status: 'CONFIRMED',
  priceCents: 8000,
  cancellationDeadline: '2026-10-10T14:00:00+02:00',
  lateCancellation: false,
};

describe('booking cancellation', () => {
  it('warns when a confirmed session is cancelled late', () => {
    expect(cancellationQuestion(CONFIRMED, new Date('2026-10-11T10:00:00+02:00'))).toContain('tardive');
    expect(cancellationQuestion(CONFIRMED, new Date('2026-10-09T10:00:00+02:00'))).toBe('Annuler ce rendez-vous ?');
  });

  it('never warns for a pending request', () => {
    const pending: AppointmentView = { ...CONFIRMED, status: 'REQUESTED' };

    expect(cancellationQuestion(pending, new Date('2026-10-12T13:00:00+02:00'))).toBe(
      'Retirer votre demande de rendez-vous ?',
    );
  });

  it('cannot cancel a session that has started', () => {
    expect(canCancel(CONFIRMED, new Date('2026-10-11T10:00:00+02:00'))).toBe(true);
    expect(canCancel(CONFIRMED, new Date('2026-10-12T14:05:00+02:00'))).toBe(false);
  });
});
