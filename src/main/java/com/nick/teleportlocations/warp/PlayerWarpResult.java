package com.nick.teleportlocations.warp;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record PlayerWarpResult(Status status, String messageKey, Optional<TeleportLocation> location) {
    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND,
        LIMIT_REACHED,
        CLAIM_DENIED
    }

    public static PlayerWarpResult created(TeleportLocation location) {
        return new PlayerWarpResult(Status.CREATED, "warp-set", Optional.of(location));
    }

    public static PlayerWarpResult updated(TeleportLocation location) {
        return new PlayerWarpResult(Status.UPDATED, "warp-updated", Optional.of(location));
    }

    public static PlayerWarpResult deleted() {
        return new PlayerWarpResult(Status.DELETED, "warp-deleted", Optional.empty());
    }

    public static PlayerWarpResult notFound() {
        return new PlayerWarpResult(Status.NOT_FOUND, "warp-missing", Optional.empty());
    }

    public static PlayerWarpResult limitReached() {
        return new PlayerWarpResult(Status.LIMIT_REACHED, "limit-reached", Optional.empty());
    }

    public static PlayerWarpResult claimDenied(String messageKey) {
        return new PlayerWarpResult(Status.CLAIM_DENIED, messageKey, Optional.empty());
    }
}
