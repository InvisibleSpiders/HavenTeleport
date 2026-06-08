package com.nick.teleportlocations.home;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HomeServiceTest {
    @Test
    void createsFirstHomeAsMainHome() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID playerId = UUID.randomUUID();

        HomeResult result = fixture.service.setHome(playerId, "base", position(), false);

        assertThat(result.status()).isEqualTo(HomeResult.Status.CREATED);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().mainHome()).isTrue();
        assertThat(fixture.service.resolveHome(playerId, "").orElseThrow().name()).isEqualTo("base");
    }

    @Test
    void deniesNewHomeWhenLimitReachedButAllowsUpdatingExistingHome() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID playerId = UUID.randomUUID();
        fixture.limits.setLimit(playerId, "home", 1);

        assertThat(fixture.service.setHome(playerId, "base", position(), false).status())
                .isEqualTo(HomeResult.Status.CREATED);
        assertThat(fixture.service.setHome(playerId, "secret", position(), false).status())
                .isEqualTo(HomeResult.Status.LIMIT_REACHED);
        assertThat(fixture.service.setHome(playerId, "base", movedPosition(), false).status())
                .isEqualTo(HomeResult.Status.UPDATED);
    }

    @Test
    void deniesHomeCreationOutsideTrustedClaim() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, false));

        HomeResult result = fixture.service.setHome(UUID.randomUUID(), "base", position(), false);

        assertThat(result.status()).isEqualTo(HomeResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("claim-denied");
    }

    @Test
    void canSetMainHomeAndDeleteHome() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID playerId = UUID.randomUUID();
        fixture.service.setHome(playerId, "base", position(), false);
        fixture.service.setHome(playerId, "secret", movedPosition(), false);

        assertThat(fixture.service.setMainHome(playerId, "secret").status()).isEqualTo(HomeResult.Status.UPDATED);
        assertThat(fixture.service.resolveHome(playerId, "").orElseThrow().name()).isEqualTo("secret");
        assertThat(fixture.service.deleteHome(playerId, "secret").status()).isEqualTo(HomeResult.Status.DELETED);
        assertThat(fixture.service.resolveHome(playerId, "secret")).isEmpty();
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(UUID.randomUUID(), "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private record Fixture(HomeService service, LimitService limits) {
        private static Fixture create(LandClaimsGateway landClaims) {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    config.categories(),
                    landClaims,
                    MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            return new Fixture(new HomeService(locationService, limitService, creationPolicy), limitService);
        }
    }
}
