package com.nick.teleportlocations.spawn;

public enum SpawnTarget {
    LAST_LOCATION,
    SPAWN,
    MAIN_HOME,
    BED_SPAWN,
    VANILLA_WORLD_SPAWN,
    DISABLED;

    public static SpawnTarget parse(String value) {
        return SpawnTarget.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
