package com.nick.teleportlocations.tpa;

import java.util.Optional;

public record TeleportCancelResult(Status status, Optional<TeleportRequest> request) {
    public enum Status {
        CANCELLED,
        NOT_FOUND
    }

    public static TeleportCancelResult cancelled(TeleportRequest request) {
        return new TeleportCancelResult(Status.CANCELLED, Optional.of(request));
    }

    public static TeleportCancelResult notFound() {
        return new TeleportCancelResult(Status.NOT_FOUND, Optional.empty());
    }
}
