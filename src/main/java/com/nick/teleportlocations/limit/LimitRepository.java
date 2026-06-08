package com.nick.teleportlocations.limit;

import java.util.Optional;
import java.util.UUID;

public interface LimitRepository {
    Optional<Integer> findLimit(UUID playerId, String category);

    void setLimit(UUID playerId, String category, int limit);

    void clearLimit(UUID playerId, String category);
}
