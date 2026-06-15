package com.nick.teleportlocations.location;

import java.util.Locale;

public final class LocationName {
    private LocationName() {
    }

    public static String normalize(String input) {
        String trimmed = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        String normalized = trimmed.replaceAll("\\s+", "_");
        if (normalized.contains(":")) {
            throw new LocationValidationException("Location names cannot contain ':'.");
        }
        return normalized;
    }
}
