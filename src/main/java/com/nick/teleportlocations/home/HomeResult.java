package com.nick.teleportlocations.home;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record HomeResult(Status status, String messageKey, Optional<TeleportLocation> location) {
    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND,
        LIMIT_REACHED,
        CLAIM_DENIED
    }

    public static HomeResult created(TeleportLocation location) {
        return new HomeResult(Status.CREATED, "home-set", Optional.of(location));
    }

    public static HomeResult updated(TeleportLocation location) {
        return new HomeResult(Status.UPDATED, "home-updated", Optional.of(location));
    }

    public static HomeResult deleted() {
        return new HomeResult(Status.DELETED, "home-deleted", Optional.empty());
    }

    public static HomeResult notFound() {
        return new HomeResult(Status.NOT_FOUND, "home-missing", Optional.empty());
    }

    public static HomeResult limitReached() {
        return new HomeResult(Status.LIMIT_REACHED, "limit-reached", Optional.empty());
    }

    public static HomeResult claimDenied(String messageKey) {
        return new HomeResult(Status.CLAIM_DENIED, messageKey, Optional.empty());
    }
}
