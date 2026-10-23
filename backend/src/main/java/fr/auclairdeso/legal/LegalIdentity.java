package fr.auclairdeso.legal;

import org.jspecify.annotations.Nullable;

/**
 * What the legal pages display. Public: anyone must be able to read these mentions without logging in
 * (article 6 III de la loi pour la confiance dans l'économie numérique).
 *
 * @param mediator null while no consumer mediator has been chosen: the pages then say so
 */
record LegalIdentity(String publisherName, String address, String siret, String contactEmail, String host,
                     @Nullable String mediator, @Nullable String mediatorUrl) {
}
