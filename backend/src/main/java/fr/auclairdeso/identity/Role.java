package fr.auclairdeso.identity;

public enum Role {
    CLIENT,
    PRACTITIONER,
    ADMIN;

    boolean isStaff() {
        return this != CLIENT;
    }
}
