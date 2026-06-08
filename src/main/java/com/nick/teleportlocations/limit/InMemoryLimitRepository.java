package com.nick.teleportlocations.limit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryLimitRepository implements LimitRepository {
    private final Map<Key, Integer> limits = new HashMap<>();

    @Override
    public Optional<Integer> findLimit(UUID playerId, String category) {
        return Optional.ofNullable(limits.get(new Key(playerId, category)));
    }

    @Override
    public void setLimit(UUID playerId, String category, int limit) {
        limits.put(new Key(playerId, category), limit);
    }

    @Override
    public void clearLimit(UUID playerId, String category) {
        limits.remove(new Key(playerId, category));
    }

    private record Key(UUID playerId, String category) {
    }
}
