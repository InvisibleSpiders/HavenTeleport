package com.nick.teleportlocations.teleportblock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeleportBlockRepository {
    Optional<TeleportBlock> findById(UUID id);

    Optional<TeleportBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ);

    List<TeleportBlock> findAll();

    void save(TeleportBlock block);

    void delete(UUID id);
}
