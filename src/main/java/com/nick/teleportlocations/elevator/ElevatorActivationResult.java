package com.nick.teleportlocations.elevator;

import java.util.Optional;

public record ElevatorActivationResult(Status status, Optional<ElevatorBlock> destination) {
    public static ElevatorActivationResult destination(ElevatorBlock block) {
        return new ElevatorActivationResult(Status.TELEPORT, Optional.of(block));
    }

    public static ElevatorActivationResult empty(Status status) {
        return new ElevatorActivationResult(status, Optional.empty());
    }

    public enum Status {
        TELEPORT,
        ACCESS_DENIED,
        NO_DESTINATION,
        COOLDOWN
    }
}
