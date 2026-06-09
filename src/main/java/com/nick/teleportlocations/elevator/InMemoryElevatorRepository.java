package com.nick.teleportlocations.elevator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryElevatorRepository implements ElevatorRepository {
    private final Map<UUID, ElevatorBlock> blocks = new HashMap<>();
    private final Map<BlockKey, UUID> blocksByPosition = new HashMap<>();
    private final Map<ColumnKey, Map<Integer, UUID>> blocksByColumn = new HashMap<>();

    @Override
    public Optional<ElevatorBlock> findById(UUID id) {
        return Optional.ofNullable(blocks.get(id));
    }

    @Override
    public Optional<ElevatorBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ) {
        return Optional.ofNullable(blocksByPosition.get(new BlockKey(worldId, blockX, blockY, blockZ)))
                .map(blocks::get);
    }

    @Override
    public List<ElevatorBlock> findColumn(UUID worldId, int blockX, int blockZ) {
        return blocksByColumn.getOrDefault(new ColumnKey(worldId, blockX, blockZ), Map.of()).values().stream()
                .map(blocks::get)
                .sorted(Comparator.comparingInt(ElevatorBlock::blockY))
                .toList();
    }

    @Override
    public List<ElevatorBlock> findAll() {
        return List.copyOf(blocks.values());
    }

    @Override
    public void save(ElevatorBlock block) {
        blocks.computeIfPresent(block.id(), (id, previous) -> {
            removeIndexes(previous);
            return previous;
        });
        blocks.put(block.id(), block);
        blocksByPosition.put(BlockKey.of(block), block.id());
        blocksByColumn.computeIfAbsent(ColumnKey.of(block), key -> new HashMap<>()).put(block.blockY(), block.id());
    }

    @Override
    public void delete(UUID id) {
        ElevatorBlock removed = blocks.remove(id);
        if (removed != null) {
            removeIndexes(removed);
        }
    }

    private void removeIndexes(ElevatorBlock block) {
        blocksByPosition.remove(BlockKey.of(block));
        ColumnKey columnKey = ColumnKey.of(block);
        Map<Integer, UUID> column = blocksByColumn.get(columnKey);
        if (column != null) {
            column.remove(block.blockY());
            if (column.isEmpty()) {
                blocksByColumn.remove(columnKey);
            }
        }
    }

    private record BlockKey(UUID worldId, int blockX, int blockY, int blockZ) {
        private static BlockKey of(ElevatorBlock block) {
            return new BlockKey(block.position().worldId(), block.blockX(), block.blockY(), block.blockZ());
        }
    }

    private record ColumnKey(UUID worldId, int blockX, int blockZ) {
        private static ColumnKey of(ElevatorBlock block) {
            return new ColumnKey(block.position().worldId(), block.blockX(), block.blockZ());
        }
    }
}
