package com.nick.teleportlocations.tpa;

import java.util.Optional;

public record TeleportRequestResult(Status status, Optional<TeleportRequest> request, long remainingCooldownSeconds) {
    public enum Status {
        REQUESTED,
        SELF_REQUEST,
        COOLDOWN,
        ALREADY_PENDING,
        TARGET_DISABLED,
        OUTGOING_LIMIT
    }

    public static TeleportRequestResult requested(TeleportRequest request) {
        return new TeleportRequestResult(Status.REQUESTED, Optional.of(request), 0);
    }

    public static TeleportRequestResult selfRequest() {
        return new TeleportRequestResult(Status.SELF_REQUEST, Optional.empty(), 0);
    }

    public static TeleportRequestResult cooldown(long remainingSeconds) {
        return new TeleportRequestResult(Status.COOLDOWN, Optional.empty(), remainingSeconds);
    }

    public static TeleportRequestResult alreadyPending() {
        return new TeleportRequestResult(Status.ALREADY_PENDING, Optional.empty(), 0);
    }

    public static TeleportRequestResult targetDisabled() {
        return new TeleportRequestResult(Status.TARGET_DISABLED, Optional.empty(), 0);
    }

    public static TeleportRequestResult outgoingLimit() {
        return new TeleportRequestResult(Status.OUTGOING_LIMIT, Optional.empty(), 0);
    }
}
