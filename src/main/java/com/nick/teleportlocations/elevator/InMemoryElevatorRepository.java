package com.nick.teleportlocations.elevator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryElevatorRepository implements ElevatorRepository {
    private final Map<UUID, ElevatorBlock> blocks = new HashMap<>();

    @Override
    public Optional<ElevatorBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ) {
        return blocks.values().stream()
                .filter(block -> matches(block, worldId, blockX, blockY, blockZ))
                .findFirst();
    }

    @Override
    public List<ElevatorBlock> findColumn(UUID worldId, int blockX, int blockZ) {
        return blocks.values().stream()
                .filter(block -> block.position().worldId().equals(worldId))
                .filter(block -> block.blockX() == blockX)
                .filter(block -> block.blockZ() == blockZ)
                .sorted(Comparator.comparingInt(ElevatorBlock::blockY))
                .toList();
    }

    @Override
    public void save(ElevatorBlock block) {
        blocks.put(block.id(), block);
    }

    @Override
    public void delete(UUID id) {
        blocks.remove(id);
    }

    private static boolean matches(ElevatorBlock block, UUID worldId, int blockX, int blockY, int blockZ) {
        return block.position().worldId().equals(worldId)
                && block.blockX() == blockX
                && block.blockY() == blockY
                && block.blockZ() == blockZ;
    }
}
