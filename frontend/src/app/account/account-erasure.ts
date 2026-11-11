/** Mirror of the backend ErasureReport record. */
export interface ErasureReport {
  readonly appointments: number;
  readonly photos: number;
  readonly phoneKept: boolean;
}

/** What the client reads once her account is gone: what was deleted, what was kept, and why. */
export function erasureSummary(report: ErasureReport): string {
  const deleted = 'Votre compte a été supprimé : votre adresse e-mail, vos données de connexion et vos photos sont effacées.';
  const why = 'la loi impose de garder ces notes dix ans, pour des raisons comptables';
  const kept = report.appointments === 0
    ? 'Aucune séance passée à conserver.'
    : report.appointments === 1
      ? `Une séance passée est conservée, avec sa note et votre nom : ${why}.`
      : `${report.appointments} séances passées sont conservées, avec leurs notes et votre nom : ${why}.`;
  const phone = report.phoneKept
    ? ' Votre numéro de téléphone reste enregistré tant que la réservation en ligne vous est bloquée.'
    : '';
  return `${deleted} ${kept}${phone}`;
}
