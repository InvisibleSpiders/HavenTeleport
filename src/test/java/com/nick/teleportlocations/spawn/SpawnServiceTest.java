package com.nick.teleportlocations.spawn;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SpawnServiceTest {
    @Test
    void storesAndResolvesServerSpawn() {
        Fixture fixture = Fixture.create();
        SavedPosition position = position("world", 0.0);

        SpawnResult result = fixture.spawn.setSpawn(position);

        assertThat(result.status()).isEqualTo(SpawnResult.Status.UPDATED);
        assertThat(fixture.spawn.spawn().orElseThrow().position()).isEqualTo(position);
    }

    @Test
    void deathResolutionPrefersMainHomeThenSpawn() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        SavedPosition spawn = position("world", 0.0);
        SavedPosition home = position("base", 25.0);
        fixture.spawn.setSpawn(spawn);
        fixture.home.setHome(playerId, "base", home, true);

        assertThat(fixture.spawn.resolve(playerId, List.of(SpawnTarget.MAIN_HOME, SpawnTarget.SPAWN))
                .orElseThrow().position()).isEqualTo(home);
    }

    @Test
    void deathResolutionFallsBackToSpawnWhenMainHomeMissing() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        SavedPosition spawn = position("world", 0.0);
        fixture.spawn.setSpawn(spawn);

        assertThat(fixture.spawn.resolve(playerId, List.of(SpawnTarget.MAIN_HOME, SpawnTarget.SPAWN))
                .orElseThrow().position()).isEqualTo(spawn);
    }

    private static SavedPosition position(String worldName, double x) {
        return new SavedPosition(UUID.randomUUID(), worldName, x, 64.0, 0.0, 0.0f, 0.0f);
    }

    private record Fixture(SpawnService spawn, HomeService home) {
        private static Fixture create() {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            HomeService homeService = new HomeService(
                    locationService,
                    limitService,
                    new CreationPolicyService(
                            config.categories(),
                            LandClaimsGateway.fixed(true, true),
                            MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
                    )
            );
            return new Fixture(new SpawnService(locationService, homeService), homeService);
        }
    }
}
