package com.nick.teleportlocations.location;

public enum VisibilityMode {
    HIDDEN,
    LISTED,
    UNLISTED;

    public static VisibilityMode parse(String value) {
        return VisibilityMode.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
