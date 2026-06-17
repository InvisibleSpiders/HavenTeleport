package com.nick.teleportlocations.location;

import java.util.Locale;

public final class LocationName {
    private LocationName() {
    }

    public static String normalize(String input) {
        String trimmed = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", "_");
    }

    public static void validateForStorage(String normalizedName) {
        if (normalizedName.contains(":")) {
            throw new LocationValidationException("Location names cannot contain ':'.");
        }
    }
}
