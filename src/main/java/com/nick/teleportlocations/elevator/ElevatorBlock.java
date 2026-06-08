package com.nick.teleportlocations.elevator;

import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.UUID;

public record ElevatorBlock(
        UUID id,
        UUID ownerId,
        SavedPosition position,
        ElevatorParticle particle,
        Instant createdAt,
        Instant updatedAt
) {
    public int blockX() {
        return block(position.x());
    }

    public int blockY() {
        return block(position.y());
    }

    public int blockZ() {
        return block(position.z());
    }

    public ElevatorBlock withParticle(ElevatorParticle particle, Instant updatedAt) {
        return new ElevatorBlock(id, ownerId, position, particle, createdAt, updatedAt);
    }

    static int block(double coordinate) {
        return (int) Math.floor(coordinate);
    }
}
