package com.nick.teleportlocations.spawn;

import java.util.List;

public record SpawnPolicy(
        SpawnTarget firstJoin,
        SpawnTarget login,
        SpawnTarget deathRespawn,
        List<SpawnTarget> deathFallback
) {
}
