package com.nick.teleportlocations.teleportblock;

import java.util.Optional;

public record TeleportBlockResult(Status status, Optional<TeleportBlock> block) {
    public enum Status {
        PLACED,
        UPDATED,
        REMOVED,
        LINKED,
        CLAIM_DENIED,
        ACCESS_DENIED,
        NOT_FOUND,
        SAME_BLOCK,
        DISTANCE_TOO_FAR
    }

    public static TeleportBlockResult of(Status status, TeleportBlock block) {
        return new TeleportBlockResult(status, Optional.of(block));
    }

    public static TeleportBlockResult empty(Status status) {
        return new TeleportBlockResult(status, Optional.empty());
    }
}
