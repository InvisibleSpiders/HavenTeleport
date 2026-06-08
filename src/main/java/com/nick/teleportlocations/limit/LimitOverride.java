package com.nick.teleportlocations.limit;

import java.util.UUID;

public record LimitOverride(UUID playerId, String category, int limit) {
}
