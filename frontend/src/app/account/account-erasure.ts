/** Mirror of the backend ErasureReport record. */
export interface ErasureReport {
  readonly appointments: number;
  readonly photos: number;
  readonly phoneKept: boolean;
}

/** What the client reads once her account is gone: what was kept, and why. */
export function erasureSummary(report: ErasureReport): string {
  const kept = report.appointments === 0
    ? 'Aucune séance passée à conserver.'
    : report.appointments === 1
      ? 'Une séance passée est conservée sans votre nom, avec sa note, que la loi impose de garder dix ans.'
      : `${report.appointments} séances passées sont conservées sans votre nom, avec leurs notes, que la loi `
      + 'impose de garder dix ans.';
  const phone = report.phoneKept
    ? ' Votre numéro de téléphone reste enregistré tant que la réservation en ligne vous est bloquée.'
    : '';
  return `Votre compte et vos informations ont été supprimés. ${kept}${phone}`;
}
