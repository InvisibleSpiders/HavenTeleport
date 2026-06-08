package com.nick.teleportlocations.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.spawn.SpawnTarget;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RespawnTargetResolverTest {
    @Test
    void picksFirstAvailableCandidate() {
        TeleportLocation home = location("home", true);
        RespawnTargetResolver resolver = new RespawnTargetResolver(
                playerId -> Optional.of(home),
                () -> Optional.of(location("spawn", false))
        );

        assertThat(resolver.resolve(UUID.randomUUID(), List.of(SpawnTarget.MAIN_HOME, SpawnTarget.SPAWN))).contains(home);
    }

    @Test
    void fallsBackWhenMainHomeMissing() {
        TeleportLocation spawn = location("spawn", false);
        RespawnTargetResolver resolver = new RespawnTargetResolver(
                playerId -> Optional.empty(),
                () -> Optional.of(spawn)
        );

        assertThat(resolver.resolve(UUID.randomUUID(), List.of(SpawnTarget.MAIN_HOME, SpawnTarget.SPAWN))).contains(spawn);
    }

    private static TeleportLocation location(String category, boolean main) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                category,
                category.equals("spawn") ? OwnerRef.server() : OwnerRef.player(UUID.randomUUID()),
                category,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                main,
                Instant.EPOCH
        );
    }
}
