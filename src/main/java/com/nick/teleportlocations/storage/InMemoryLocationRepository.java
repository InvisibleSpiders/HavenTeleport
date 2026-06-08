package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.TeleportLocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryLocationRepository implements LocationRepository {
    private final Map<UUID, TeleportLocation> locations = new LinkedHashMap<>();

    @Override
    public Optional<TeleportLocation> findById(UUID id) {
        return Optional.ofNullable(locations.get(id));
    }

    @Override
    public Optional<TeleportLocation> findByIdentity(OwnerRef owner, String category, String normalizedName) {
        return locations.values().stream()
                .filter(location -> location.owner().equals(owner))
                .filter(location -> location.category().equals(category))
                .filter(location -> location.normalizedName().equals(normalizedName))
                .findFirst();
    }

    @Override
    public List<TeleportLocation> findByOwnerAndCategory(OwnerRef owner, String category) {
        return locations.values().stream()
                .filter(location -> location.owner().equals(owner))
                .filter(location -> location.category().equals(category))
                .toList();
    }

    @Override
    public List<TeleportLocation> findByCategory(String category) {
        return locations.values().stream()
                .filter(location -> location.category().equals(category))
                .toList();
    }

    @Override
    public void save(TeleportLocation location) {
        locations.put(location.id(), location);
    }

    @Override
    public void delete(UUID id) {
        locations.remove(id);
    }
}
