package com.nick.teleportlocations.elevator;

import java.util.Optional;

public record ElevatorActivationResult(Status status, Optional<ElevatorBlock> destination, long remainingCooldownSeconds) {
    public static ElevatorActivationResult destination(ElevatorBlock block) {
        return new ElevatorActivationResult(Status.TELEPORT, Optional.of(block), 0);
    }

    public static ElevatorActivationResult empty(Status status) {
        return new ElevatorActivationResult(status, Optional.empty(), 0);
    }

    public static ElevatorActivationResult cooldown(long remainingSeconds) {
        return new ElevatorActivationResult(Status.COOLDOWN, Optional.empty(), Math.max(0, remainingSeconds));
    }

    public enum Status {
        TELEPORT,
        ACCESS_DENIED,
        NO_DESTINATION,
        COOLDOWN
    }
}
