package com.nick.teleportlocations.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostType;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ShopWarpServiceTest {
    @Test
    void createsFreePublicListedShopWarp() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();

        ShopWarpResult result = fixture.service.setShop(owner, "tools", position(), false);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.CREATED);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(result.location().orElseThrow().visibilityMode()).isEqualTo(VisibilityMode.LISTED);
        assertThat(result.location().orElseThrow().cost().type()).isEqualTo(CostType.FREE);
        assertThat(fixture.service.visibleShops(UUID.randomUUID())).extracting("name").contains("tools");
    }

    @Test
    void deniesNewShopWhenLimitReachedButAllowsUpdatingExistingShop() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.limits.setLimit(owner, "shop", 1);

        assertThat(fixture.service.setShop(owner, "tools", position(), false).status())
                .isEqualTo(ShopWarpResult.Status.CREATED);
        assertThat(fixture.service.setShop(owner, "food", position(), false).status())
                .isEqualTo(ShopWarpResult.Status.LIMIT_REACHED);
        assertThat(fixture.service.setShop(owner, "tools", movedPosition(), false).status())
                .isEqualTo(ShopWarpResult.Status.UPDATED);
    }

    @Test
    void deniesShopCreationOutsideTrustedClaim() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, false));

        ShopWarpResult result = fixture.service.setShop(UUID.randomUUID(), "tools", position(), false);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("claim-denied");
    }

    @Test
    void resolvesVisibleShopAndDeletesOwnShop() {
        Fixture fixture = Fixture.create(LandClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), false);

        assertThat(fixture.service.resolveVisibleShop(viewer, "tools")).isPresent();
        assertThat(fixture.service.deleteShop(owner, "tools").status()).isEqualTo(ShopWarpResult.Status.DELETED);
        assertThat(fixture.service.resolveVisibleShop(viewer, "tools")).isEmpty();
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(UUID.randomUUID(), "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private record Fixture(ShopWarpService service, LimitService limits) {
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
            return new Fixture(new ShopWarpService(locationService, limitService, creationPolicy), limitService);
        }
    }
}
