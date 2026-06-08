package com.nick.teleportlocations.spawn;

import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.CreateLocationRequest;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SpawnService {
    private static final String CATEGORY = "spawn";
    private static final String NAME = "spawn";

    private final LocationService locations;
    private final HomeService homes;

    public SpawnService(LocationService locations, HomeService homes) {
        this.locations = locations;
        this.homes = homes;
    }

    public SpawnResult setSpawn(SavedPosition position) {
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                OwnerRef.server(),
                NAME,
                position,
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                false
        ));
        return SpawnResult.updated(location);
    }

    public Optional<TeleportLocation> spawn() {
        return locations.find(OwnerRef.server(), CATEGORY, NAME);
    }

    public Optional<TeleportLocation> resolve(UUID playerId, List<SpawnTarget> candidates) {
        for (SpawnTarget candidate : candidates) {
            if (candidate == SpawnTarget.MAIN_HOME) {
                Optional<TeleportLocation> home = homes.resolveHome(playerId, "");
                if (home.isPresent()) {
                    return home;
                }
            }
            if (candidate == SpawnTarget.SPAWN) {
                Optional<TeleportLocation> configuredSpawn = spawn();
                if (configuredSpawn.isPresent()) {
                    return configuredSpawn;
                }
            }
        }
        return Optional.empty();
    }
}
