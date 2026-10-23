package fr.auclairdeso.booking;

/**
 * A client erased her account: nothing identifies her in the booking module any more, and her login
 * must go too (docs/specification.md, 9).
 */
public record ClientErased(String email) {
}
