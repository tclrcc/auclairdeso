package fr.auclairdeso.notification;

/** An email, before it gets its sender and its recipient. */
record Message(String subject, String text) {
}
