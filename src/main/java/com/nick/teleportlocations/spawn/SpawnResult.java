package com.nick.teleportlocations.spawn;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record SpawnResult(Status status, String messageKey, Optional<TeleportLocation> location) {
    public enum Status {
        UPDATED,
        NOT_FOUND
    }

    public static SpawnResult updated(TeleportLocation location) {
        return new SpawnResult(Status.UPDATED, "spawn-set", Optional.of(location));
    }

    public static SpawnResult notFound() {
        return new SpawnResult(Status.NOT_FOUND, "spawn-missing", Optional.empty());
    }
}
