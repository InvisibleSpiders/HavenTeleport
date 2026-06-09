package com.nick.teleportlocations.tpa;

import java.util.Optional;

public record TeleportAcceptResult(Status status, Optional<TeleportRequest> request) {
    public enum Status {
        ACCEPTED,
        NOT_FOUND
    }

    public static TeleportAcceptResult accepted(TeleportRequest request) {
        return new TeleportAcceptResult(Status.ACCEPTED, Optional.of(request));
    }

    public static TeleportAcceptResult notFound() {
        return new TeleportAcceptResult(Status.NOT_FOUND, Optional.empty());
    }
}
