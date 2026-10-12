package fr.auclairdeso.billing;

/** How a client paid (docs/specification.md, 3.5). */
enum PaymentMethod {
    TRANSFER("Virement"),
    CASH("Espèces"),
    /** Wero, Lydia, PayPal... */
    MOBILE("Paiement mobile");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /** The word of the receipt book. */
    String label() {
        return label;
    }
}
