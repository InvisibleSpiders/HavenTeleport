package com.nick.teleportlocations.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.claim.MissingHavenClaimsPolicy;
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
    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOVED_WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void createsFreePublicListedShopWarp() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
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
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
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
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, false));

        ShopWarpResult result = fixture.service.setShop(UUID.randomUUID(), "tools", position(), false);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("claim-denied");
    }

    @Test
    void rejectsInvalidShopNamesWithoutThrowing() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), false);

        ShopWarpResult create = fixture.service.setShop(owner, "tools:west", movedPosition(), false);
        ShopWarpResult rename = fixture.service.rename(owner, "tools", "tools:west");

        assertThat(create.status()).isEqualTo(ShopWarpResult.Status.INVALID_NAME);
        assertThat(rename.status()).isEqualTo(ShopWarpResult.Status.INVALID_NAME);
        assertThat(fixture.service.ownerShops(owner)).extracting("name").containsExactly("tools");
        assertThat(fixture.service.resolveVisibleShop(owner, "tools:west")).isEmpty();
    }

    @Test
    void resolvesVisibleShopAndDeletesOwnShop() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), false);

        assertThat(fixture.service.resolveVisibleShop(viewer, "tools")).isPresent();
        assertThat(fixture.service.deleteShop(owner, "tools").status()).isEqualTo(ShopWarpResult.Status.DELETED);
        assertThat(fixture.service.resolveVisibleShop(viewer, "tools")).isEmpty();
    }

    @Test
    void renamesShopWithoutChangingForcedSettings() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), false);

        ShopWarpResult result = fixture.service.rename(owner, "tools", "gear");

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.UPDATED);
        assertThat(fixture.service.resolveVisibleShop(owner, "tools")).isEmpty();
        assertThat(fixture.service.resolveVisibleShop(owner, "gear")).isPresent();
        assertThat(fixture.service.resolveVisibleShop(owner, "gear").orElseThrow().accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(fixture.service.resolveVisibleShop(owner, "gear").orElseThrow().visibilityMode()).isEqualTo(VisibilityMode.LISTED);
    }

    @Test
    void renameShopRejectsMissingAndDuplicateNames() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        UUID owner = UUID.randomUUID();
        fixture.limits.setLimit(owner, "shop", 2);
        fixture.service.setShop(owner, "tools", position(), false);
        fixture.service.setShop(owner, "gear", movedPosition(), false);

        ShopWarpResult missing = fixture.service.rename(owner, "unknown", "new-name");
        ShopWarpResult duplicate = fixture.service.rename(owner, "tools", "gear");

        assertThat(missing.status()).isEqualTo(ShopWarpResult.Status.NOT_FOUND);
        assertThat(duplicate.status()).isEqualTo(ShopWarpResult.Status.DUPLICATE_NAME);
        assertThat(fixture.service.resolveVisibleShop(owner, "tools")).isPresent();
        assertThat(fixture.service.resolveVisibleShop(owner, "gear").orElseThrow().position()).isEqualTo(movedPosition());
    }

    @Test
    void renameShopAllowsSameNormalizedName() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), false);

        ShopWarpResult result = fixture.service.rename(owner, "tools", "Tools");

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.UPDATED);
        assertThat(fixture.service.resolveVisibleShop(owner, "tools")).isPresent();
        assertThat(fixture.service.ownerShops(owner)).hasSize(1);
        assertThat(fixture.service.ownerShops(owner).getFirst().name()).isEqualTo("Tools");
        assertThat(fixture.service.ownerShops(owner).getFirst().accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(fixture.service.ownerShops(owner).getFirst().visibilityMode()).isEqualTo(VisibilityMode.LISTED);
    }

    @Test
    void relocateShopRequiresOwnedPubliclyAccessibleClaim() {
        Fixture allowed = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        UUID owner = UUID.randomUUID();
        allowed.service.setShop(owner, "tools", position(), false);

        ShopWarpResult result = allowed.service.relocate(owner, "tools", movedPosition(), false);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.UPDATED);
        assertThat(allowed.service.resolveVisibleShop(owner, "tools").orElseThrow().position()).isEqualTo(movedPosition());
    }

    @Test
    void relocateShopRejectsUnownedOrPrivateClaimAndWilderness() {
        UUID owner = UUID.randomUUID();
        Fixture unowned = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, false));
        unowned.service.setShop(owner, "tools", position(), true);
        Fixture privateClaim = Fixture.create(HavenClaimsGateway.fixedOwned(true, false, true));
        privateClaim.service.setShop(owner, "tools", position(), true);
        Fixture wilderness = Fixture.create(HavenClaimsGateway.fixedOwned(false, false, false));
        wilderness.service.setShop(owner, "tools", position(), true);

        assertThat(unowned.service.relocate(owner, "tools", movedPosition(), false).status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
        assertThat(privateClaim.service.relocate(owner, "tools", movedPosition(), false).status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
        assertThat(wilderness.service.relocate(owner, "tools", movedPosition(), false).status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
    }

    @Test
    void relocateShopDeniesWhenHavenClaimsMissing() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.missing());
        UUID owner = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), true);

        ShopWarpResult result = fixture.service.relocate(owner, "tools", movedPosition(), false);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("havenclaims-missing");
    }

    @Test
    void relocateShopAllowsAdminBypassWhenClaimChecksFail() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixedOwned(false, false, false));
        UUID owner = UUID.randomUUID();
        fixture.service.setShop(owner, "tools", position(), true);

        ShopWarpResult result = fixture.service.relocate(owner, "tools", movedPosition(), true);

        assertThat(result.status()).isEqualTo(ShopWarpResult.Status.UPDATED);
        assertThat(fixture.service.resolveVisibleShop(owner, "tools").orElseThrow().position()).isEqualTo(movedPosition());
    }

    private static SavedPosition position() {
        return new SavedPosition(WORLD_ID, "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(MOVED_WORLD_ID, "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private record Fixture(ShopWarpService service, LimitService limits) {
        private static Fixture create(HavenClaimsGateway havenClaims) {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    config.categories(),
                    havenClaims,
                    MissingHavenClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            return new Fixture(new ShopWarpService(locationService, limitService, creationPolicy), limitService);
        }
    }
}
