package fr.auclairdeso.catalog;

/**
 * Quand et combien le client paie la séance
 */
public enum PaymentPolicy {
    /** Paiement en ligne lors de la réservation */
    FULL_ONLINE,

    /** Avance payée en ligne, puis le reste en présence */
    DEPOSIT_ONLINE,

    /** Paiement en présence */
    ON_SITE
}
