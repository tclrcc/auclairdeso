package fr.auclairdeso.catalog;

/**
 * Mode de consultation possible
 */
public enum ConsultationMode {
    /** Domicile de la practicienne, que pour les personnes de confiance. Pas d'adresse dévoilée */
    IN_PERSON,

    /** Au domicile du client : à 20km de Chazey/Ain */
    CLIENT_HOME,

    /** Appel vidéo via Messenger */
    VIDEO,

    /** Le client appelle la practicienne */
    PHONE
}
