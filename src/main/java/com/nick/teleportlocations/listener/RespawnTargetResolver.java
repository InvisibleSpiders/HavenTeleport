package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.spawn.SpawnTarget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RespawnTargetResolver {
    private final Function<UUID, Optional<TeleportLocation>> mainHomeLookup;
    private final Supplier<Optional<TeleportLocation>> spawnLookup;

    public RespawnTargetResolver(
            Function<UUID, Optional<TeleportLocation>> mainHomeLookup,
            Supplier<Optional<TeleportLocation>> spawnLookup
    ) {
        this.mainHomeLookup = mainHomeLookup;
        this.spawnLookup = spawnLookup;
    }

    public Optional<TeleportLocation> resolve(UUID playerId, List<SpawnTarget> candidates) {
        for (SpawnTarget candidate : candidates) {
            if (candidate == SpawnTarget.MAIN_HOME) {
                Optional<TeleportLocation> home = mainHomeLookup.apply(playerId);
                if (home.isPresent()) {
                    return home;
                }
            }
            if (candidate == SpawnTarget.SPAWN) {
                Optional<TeleportLocation> spawn = spawnLookup.get();
                if (spawn.isPresent()) {
                    return spawn;
                }
            }
        }
        return Optional.empty();
    }
}
