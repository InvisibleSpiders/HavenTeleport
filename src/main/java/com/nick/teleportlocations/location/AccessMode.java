package com.nick.teleportlocations.location;

public enum AccessMode {
    PRIVATE,
    TRUSTED,
    PUBLIC;

    public static AccessMode parse(String value) {
        return AccessMode.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
