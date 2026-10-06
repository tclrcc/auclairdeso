/** Mirror of the backend CurrentUser record. */
export interface CurrentUser {
  readonly email: string;
  readonly roles: readonly string[];
  /** How the user proved their identity in this session: 'OTT' (magic link), 'PASSWORD'. */
  readonly factors: readonly string[];
  readonly passwordSet: boolean;
}

const STAFF_ROLES = ['PRACTITIONER', 'ADMIN'];

export function isStaff(user: CurrentUser): boolean {
  return user.roles.some((role) => STAFF_ROLES.includes(role));
}

export function hasBothFactors(user: CurrentUser): boolean {
  return user.factors.includes('OTT') && user.factors.includes('PASSWORD');
}
