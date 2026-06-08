package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.location.LocationName;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.storage.LocationRepository;
import java.util.Optional;
import java.util.UUID;

public final class TeleportDestinationResolver {
    private final LocationRepository repository;

    public TeleportDestinationResolver(LocationRepository repository) {
        this.repository = repository;
    }

    public Optional<TeleportLocation> resolveHome(UUID playerId, String name) {
        if (name == null || name.isBlank()) {
            return repository.findByOwnerAndCategory(OwnerRef.player(playerId), "home").stream()
                    .filter(TeleportLocation::mainHome)
                    .findFirst();
        }
        return repository.findByIdentity(OwnerRef.player(playerId), "home", LocationName.normalize(name));
    }
}
