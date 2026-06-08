package com.nick.teleportlocations.category;

public enum OwnerKind {
    PLAYER,
    SERVER;

    public static OwnerKind parse(String value) {
        return OwnerKind.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
