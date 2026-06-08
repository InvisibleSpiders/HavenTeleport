package com.nick.teleportlocations.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportDestinationResolverTest {
    @Test
    void homeWithoutNameUsesMainHome() {
        UUID playerId = UUID.randomUUID();
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        TeleportLocation main = location(playerId, "home", "base", true);
        repository.save(main);

        TeleportDestinationResolver resolver = new TeleportDestinationResolver(repository);

        assertThat(resolver.resolveHome(playerId, "")).contains(main);
    }

    @Test
    void namedHomeUsesNormalizedName() {
        UUID playerId = UUID.randomUUID();
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        TeleportLocation mine = location(playerId, "home", "Deep Mine", false);
        repository.save(mine);

        TeleportDestinationResolver resolver = new TeleportDestinationResolver(repository);

        assertThat(resolver.resolveHome(playerId, "deep_mine")).contains(mine);
    }

    private static TeleportLocation location(UUID ownerId, String category, String name, boolean main) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                category,
                OwnerRef.player(ownerId),
                name,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                main,
                Instant.EPOCH
        );
    }
}
