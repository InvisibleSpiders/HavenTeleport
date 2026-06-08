package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.TeleportLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository {
    Optional<TeleportLocation> findById(UUID id);

    Optional<TeleportLocation> findByIdentity(OwnerRef owner, String category, String normalizedName);

    List<TeleportLocation> findByOwnerAndCategory(OwnerRef owner, String category);

    List<TeleportLocation> findByCategory(String category);

    void save(TeleportLocation location);

    void delete(UUID id);
}
