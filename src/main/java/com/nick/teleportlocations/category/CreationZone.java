package com.nick.teleportlocations.category;

public enum CreationZone {
    TRUSTED_CLAIM,
    WILDERNESS,
    ANYWHERE,
    ADMIN;

    public static CreationZone parse(String value) {
        return CreationZone.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
