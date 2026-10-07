package fr.auclairdeso.catalog;

/**
 * Families of offerings (docs/specification.md, 3.1).
 */
public enum OfferingCategory {

    /** Voyance: the main activity. */
    CLAIRVOYANCE,

    /** Magnétisme. */
    MAGNETISM,

    /** Rééquilibrage énergétique. */
    ENERGY_REBALANCING;

    /** Clairvoyance sessions need a portrait photo of the client (docs/specification.md, 3.3). */
    public boolean requiresPhoto() {
        return this == CLAIRVOYANCE;
    }
}
