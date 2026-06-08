package com.nick.teleportlocations.location;

import com.nick.teleportlocations.storage.LocationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class LocationService {
    private final LocationRepository repository;
    private final Supplier<Instant> clock;

    public LocationService(LocationRepository repository, Supplier<Instant> clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public TeleportLocation createOrUpdate(CreateLocationRequest request) {
        Instant now = clock.get();
        String normalizedName = LocationName.normalize(request.name());
        Optional<TeleportLocation> existing = repository.findByIdentity(request.owner(), request.category(), normalizedName);
        TeleportLocation location = TeleportLocation.create(
                existing.map(TeleportLocation::id).orElseGet(UUID::randomUUID),
                request.category(),
                request.owner(),
                request.name(),
                request.position(),
                request.accessMode(),
                request.visibilityMode(),
                request.cost(),
                request.mainHome(),
                existing.map(TeleportLocation::createdAt).orElse(now)
        );
        location = new TeleportLocation(
                location.id(),
                location.category(),
                location.owner(),
                location.name(),
                location.normalizedName(),
                location.position(),
                location.accessMode(),
                location.visibilityMode(),
                location.cost(),
                location.mainHome(),
                existing.map(TeleportLocation::createdAt).orElse(now),
                now
        );
        if (location.mainHome()) {
            unsetOtherMainHomes(location);
        }
        repository.save(location);
        return location;
    }

    public Optional<TeleportLocation> mainHome(UUID playerId) {
        return repository.findByOwnerAndCategory(OwnerRef.player(playerId), "home").stream()
                .filter(TeleportLocation::mainHome)
                .findFirst();
    }

    public Optional<TeleportLocation> find(OwnerRef owner, String category, String name) {
        return repository.findByIdentity(owner, category, LocationName.normalize(name));
    }

    public List<TeleportLocation> list(OwnerRef owner, String category) {
        return repository.findByOwnerAndCategory(owner, category);
    }

    public void delete(UUID id) {
        repository.delete(id);
    }

    private void unsetOtherMainHomes(TeleportLocation mainHome) {
        repository.findByOwnerAndCategory(mainHome.owner(), "home").stream()
                .filter(location -> !location.id().equals(mainHome.id()))
                .filter(TeleportLocation::mainHome)
                .map(location -> location.withMainHome(false, clock.get()))
                .forEach(repository::save);
    }
}
