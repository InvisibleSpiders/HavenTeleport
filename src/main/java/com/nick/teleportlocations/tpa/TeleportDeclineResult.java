package com.nick.teleportlocations.tpa;

import java.util.Optional;

public record TeleportDeclineResult(Status status, Optional<TeleportRequest> request) {
    public enum Status {
        DECLINED,
        NOT_FOUND
    }

    public static TeleportDeclineResult declined(TeleportRequest request) {
        return new TeleportDeclineResult(Status.DECLINED, Optional.of(request));
    }

    public static TeleportDeclineResult notFound() {
        return new TeleportDeclineResult(Status.NOT_FOUND, Optional.empty());
    }
}
