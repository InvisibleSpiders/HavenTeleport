package com.nick.teleportlocations.teleportblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryTeleportBlockRepository implements TeleportBlockRepository {
    private final Map<UUID, TeleportBlock> blocks = new HashMap<>();

    @Override
    public Optional<TeleportBlock> findById(UUID id) {
        return Optional.ofNullable(blocks.get(id));
    }

    @Override
    public Optional<TeleportBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ) {
        return blocks.values().stream()
                .filter(block -> block.position().worldId().equals(worldId))
                .filter(block -> block.blockX() == blockX)
                .filter(block -> block.blockY() == blockY)
                .filter(block -> block.blockZ() == blockZ)
                .findFirst();
    }

    @Override
    public List<TeleportBlock> findAll() {
        return List.copyOf(new ArrayList<>(blocks.values()));
    }

    @Override
    public void save(TeleportBlock block) {
        blocks.put(block.id(), block);
    }

    @Override
    public void delete(UUID id) {
        blocks.remove(id);
    }
}
