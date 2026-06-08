package com.nick.teleportlocations.shop;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record ShopWarpResult(Status status, String messageKey, Optional<TeleportLocation> location) {
    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND,
        LIMIT_REACHED,
        CLAIM_DENIED
    }

    public static ShopWarpResult created(TeleportLocation location) {
        return new ShopWarpResult(Status.CREATED, "shop-set", Optional.of(location));
    }

    public static ShopWarpResult updated(TeleportLocation location) {
        return new ShopWarpResult(Status.UPDATED, "shop-updated", Optional.of(location));
    }

    public static ShopWarpResult deleted() {
        return new ShopWarpResult(Status.DELETED, "shop-deleted", Optional.empty());
    }

    public static ShopWarpResult notFound() {
        return new ShopWarpResult(Status.NOT_FOUND, "shop-missing", Optional.empty());
    }

    public static ShopWarpResult limitReached() {
        return new ShopWarpResult(Status.LIMIT_REACHED, "limit-reached", Optional.empty());
    }

    public static ShopWarpResult claimDenied(String messageKey) {
        return new ShopWarpResult(Status.CLAIM_DENIED, messageKey, Optional.empty());
    }
}
