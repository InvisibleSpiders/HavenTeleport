package com.nick.teleportlocations.outpost;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record OutpostResult(Status status, String messageKey, Optional<TeleportLocation> location) {
    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND,
        LIMIT_REACHED,
        CLAIM_DENIED
    }

    public static OutpostResult created(TeleportLocation location) {
        return new OutpostResult(Status.CREATED, "outpost-set", Optional.of(location));
    }

    public static OutpostResult updated(TeleportLocation location) {
        return new OutpostResult(Status.UPDATED, "outpost-updated", Optional.of(location));
    }

    public static OutpostResult deleted() {
        return new OutpostResult(Status.DELETED, "outpost-deleted", Optional.empty());
    }

    public static OutpostResult notFound() {
        return new OutpostResult(Status.NOT_FOUND, "outpost-missing", Optional.empty());
    }

    public static OutpostResult limitReached() {
        return new OutpostResult(Status.LIMIT_REACHED, "limit-reached", Optional.empty());
    }

    public static OutpostResult claimDenied(String messageKey) {
        return new OutpostResult(Status.CLAIM_DENIED, messageKey, Optional.empty());
    }
}
