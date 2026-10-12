package fr.auclairdeso.billing;

/** A bill was issued for a completed session: the client receives it by email. */
public record BillIssued(BillNotice bill) {
}
