package com.nick.teleportlocations.elevator;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ElevatorActivationService {
    private final ElevatorService elevators;
    private final ElevatorCooldownService cooldowns;
    private final int maxDistance;

    public ElevatorActivationService(ElevatorService elevators, ElevatorCooldownService cooldowns, int maxDistance) {
        this.elevators = Objects.requireNonNull(elevators, "elevators");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.maxDistance = Math.max(1, maxDistance);
    }

    public ElevatorActivationResult activate(
            UUID playerId,
            SavedPosition source,
            ElevatorDirection direction,
            boolean adminBypassClaims,
            boolean bypassCooldown
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(direction, "direction");
        if (!elevators.canUse(playerId, source, adminBypassClaims)) {
            return ElevatorActivationResult.empty(ElevatorActivationResult.Status.ACCESS_DENIED);
        }
        Optional<ElevatorBlock> destination = elevators.findDestination(source, direction, maxDistance);
        if (destination.isEmpty()) {
            return ElevatorActivationResult.empty(ElevatorActivationResult.Status.NO_DESTINATION);
        }
        if (!cooldowns.tryUse(playerId, bypassCooldown)) {
            return ElevatorActivationResult.cooldown(cooldowns.remainingSeconds(playerId));
        }
        return ElevatorActivationResult.destination(destination.get());
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.clear(playerId);
    }
}
