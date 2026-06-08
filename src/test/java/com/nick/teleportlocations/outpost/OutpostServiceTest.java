package com.nick.teleportlocations.outpost;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class OutpostServiceTest {
    @Test
    void createsPrivateHiddenOutpostInWilderness() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(false, false));
        UUID owner = UUID.randomUUID();

        OutpostResult result = fixture.service.setOutpost(owner, "camp", position(), false);

        assertThat(result.status()).isEqualTo(OutpostResult.Status.CREATED);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().accessMode()).isEqualTo(AccessMode.PRIVATE);
        assertThat(result.location().orElseThrow().visibilityMode()).isEqualTo(VisibilityMode.HIDDEN);
        assertThat(fixture.service.resolveOutpost(owner, "camp")).isPresent();
    }

    @Test
    void deniesOutpostCreationInsideClaim() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));

        OutpostResult result = fixture.service.setOutpost(UUID.randomUUID(), "camp", position(), false);

        assertThat(result.status()).isEqualTo(OutpostResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("claimed-land");
    }

    @Test
    void deniesNewOutpostWhenLimitReachedButAllowsUpdatingExistingOutpost() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(false, false));
        UUID owner = UUID.randomUUID();
        fixture.limits.setLimit(owner, "outpost", 1);

        assertThat(fixture.service.setOutpost(owner, "camp", position(), false).status())
                .isEqualTo(OutpostResult.Status.CREATED);
        assertThat(fixture.service.setOutpost(owner, "mine", position(), false).status())
                .isEqualTo(OutpostResult.Status.LIMIT_REACHED);
        assertThat(fixture.service.setOutpost(owner, "camp", movedPosition(), false).status())
                .isEqualTo(OutpostResult.Status.UPDATED);
    }

    @Test
    void deletesOwnOutpost() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(false, false));
        UUID owner = UUID.randomUUID();
        fixture.service.setOutpost(owner, "camp", position(), false);

        assertThat(fixture.service.deleteOutpost(owner, "camp").status()).isEqualTo(OutpostResult.Status.DELETED);
        assertThat(fixture.service.resolveOutpost(owner, "camp")).isEmpty();
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(UUID.randomUUID(), "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private record Fixture(OutpostService service, LimitService limits) {
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
            return new Fixture(new OutpostService(locationService, limitService, creationPolicy), limitService);
        }
    }
}
