package com.nick.teleportlocations.elevator;

import java.util.Optional;

public record ElevatorResult(Status status, Optional<ElevatorBlock> block) {
    public static ElevatorResult of(Status status, ElevatorBlock block) {
        return new ElevatorResult(status, Optional.of(block));
    }

    public static ElevatorResult empty(Status status) {
        return new ElevatorResult(status, Optional.empty());
    }

    public enum Status {
        PLACED,
        UPDATED,
        REMOVED,
        NOT_FOUND,
        CLAIM_DENIED,
        ACCESS_DENIED
    }
}
