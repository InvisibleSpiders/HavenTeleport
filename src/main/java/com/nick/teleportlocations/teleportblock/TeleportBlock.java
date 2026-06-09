package com.nick.teleportlocations.teleportblock;

import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record TeleportBlock(
        UUID id,
        UUID ownerId,
        SavedPosition position,
        Optional<UUID> linkedBlockId,
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

    public TeleportBlock withLink(Optional<UUID> linkedBlockId, Instant updatedAt) {
        return new TeleportBlock(id, ownerId, position, linkedBlockId, createdAt, updatedAt);
    }

    static int block(double coordinate) {
        return (int) Math.floor(coordinate);
    }
}
