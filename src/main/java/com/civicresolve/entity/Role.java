package com.civicresolve.entity;

public enum Role {
    ROLE_CITIZEN("Citizen"),
    ROLE_OFFICER("Officer"),
    ROLE_ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
