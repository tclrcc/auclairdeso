import { AppointmentView } from './booking';

/** A request or a confirmed session that has not started yet. */
export function canCancel(appointment: AppointmentView, now: Date): boolean {
  return (
    (appointment.status === 'REQUESTED' || appointment.status === 'CONFIRMED') &&
    new Date(appointment.start).getTime() > now.getTime()
  );
}

/** The question asked before cancelling, with a warning when it will count as late. */
export function cancellationQuestion(appointment: AppointmentView, now: Date): string {
  if (appointment.status === 'REQUESTED') {
    return 'Retirer votre demande de rendez-vous ?';
  }
  if (now.getTime() >= new Date(appointment.cancellationDeadline).getTime()) {
    return (
      'Votre séance a lieu dans moins de 48 heures : cette annulation sera comptée comme tardive. ' +
      "Après deux annulations tardives, la réservation en ligne n'est plus possible. Annuler quand même ?"
    );
  }
  return 'Annuler ce rendez-vous ?';
}
