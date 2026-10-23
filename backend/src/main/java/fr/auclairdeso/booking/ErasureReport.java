package fr.auclairdeso.booking;

/**
 * What the erasure did, so that the client reads it rather than guesses it.
 *
 * @param appointments sessions kept without her name, because their bills are kept ten years
 * @param photos portraits deleted
 * @param phoneKept whether her phone number stays, which happens only while she is blocked (6)
 */
record ErasureReport(int appointments, int photos, boolean phoneKept) {
}
