package com.nick.teleportlocations.elevator;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class ElevatorCooldownService {
    private final int cooldownSeconds;
    private final Supplier<Instant> clock;
    private final Map<UUID, Instant> nextUseByPlayer = new HashMap<>();

    public ElevatorCooldownService(int cooldownSeconds, Supplier<Instant> clock) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean tryUse(UUID playerId, boolean bypassCooldown) {
        Objects.requireNonNull(playerId, "playerId");
        if (bypassCooldown) {
            return true;
        }
        Instant now = clock.get();
        Instant nextUse = nextUseByPlayer.get(playerId);
        if (nextUse != null && now.isBefore(nextUse)) {
            return false;
        }
        nextUseByPlayer.put(playerId, now.plusSeconds(cooldownSeconds));
        return true;
    }

    public long remainingSeconds(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Instant nextUse = nextUseByPlayer.get(playerId);
        if (nextUse == null) {
            return 0;
        }
        long seconds = Duration.between(clock.get(), nextUse).toSeconds();
        return Math.max(0, seconds);
    }

    public void clear(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        nextUseByPlayer.remove(playerId);
    }
}
