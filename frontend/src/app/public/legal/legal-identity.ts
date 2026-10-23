import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

/** The day the legal texts were last revised. It belongs to the texts, so it lives with them. */
export const LEGAL_UPDATED_ON = '19 octobre 2026';

/** Mirror of the backend LegalIdentity record. */
export interface LegalIdentity {
  readonly publisherName: string;
  readonly address: string;
  readonly siret: string;
  readonly contactEmail: string;
  readonly host: string;
  /** Null while no consumer mediator has been chosen: the pages say so rather than invent one. */
  readonly mediator: string | null;
  readonly mediatorUrl: string | null;
}

/**
 * Who publishes the site. Read once and shared by the three legal pages, so that moving from one to
 * another asks nothing again.
 */
@Service()
export class LegalIdentityStore {
  readonly identity = httpResource<LegalIdentity>(() => '/api/legal');
}
