/** Mirror of the backend CurrentUser record. */
export interface CurrentUser {
  readonly email: string;
  readonly roles: readonly string[];
}
