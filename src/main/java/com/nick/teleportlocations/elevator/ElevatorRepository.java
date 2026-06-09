package com.nick.teleportlocations.elevator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElevatorRepository {
    Optional<ElevatorBlock> findById(UUID id);

    Optional<ElevatorBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ);

    List<ElevatorBlock> findColumn(UUID worldId, int blockX, int blockZ);

    List<ElevatorBlock> findAll();

    void save(ElevatorBlock block);

    void delete(UUID id);
}
