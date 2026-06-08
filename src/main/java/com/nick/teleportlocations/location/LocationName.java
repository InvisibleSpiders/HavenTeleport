package com.nick.teleportlocations.location;

import java.util.Locale;

public final class LocationName {
    private LocationName() {
    }

    public static String normalize(String input) {
        String trimmed = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", "_");
    }
}
