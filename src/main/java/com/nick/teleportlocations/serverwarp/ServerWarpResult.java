package com.nick.teleportlocations.serverwarp;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record ServerWarpResult(Status status, Optional<TeleportLocation> location) {
    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND
    }

    public static ServerWarpResult created(TeleportLocation location) {
        return new ServerWarpResult(Status.CREATED, Optional.of(location));
    }

    public static ServerWarpResult updated(TeleportLocation location) {
        return new ServerWarpResult(Status.UPDATED, Optional.of(location));
    }

    public static ServerWarpResult deleted() {
        return new ServerWarpResult(Status.DELETED, Optional.empty());
    }

    public static ServerWarpResult notFound() {
        return new ServerWarpResult(Status.NOT_FOUND, Optional.empty());
    }
}
