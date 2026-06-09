package com.nick.teleportlocations.tpa;

import java.time.Instant;
import java.util.UUID;

public record TeleportRequest(
        UUID id,
        UUID requesterId,
        UUID targetId,
        TeleportRequestType type,
        Instant createdAt,
        Instant expiresAt
) {
}
