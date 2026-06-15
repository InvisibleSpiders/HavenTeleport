package com.nick.teleportlocations.warp;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.claim.MissingHavenClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerWarpServiceTest {
    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOVED_WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void createsPublicListedWarp() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();

        PlayerWarpResult result = fixture.service.setWarp(owner, "market", position(), false);

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.CREATED);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(result.location().orElseThrow().visibilityMode()).isEqualTo(VisibilityMode.LISTED);
        assertThat(fixture.service.visibleWarps(UUID.randomUUID())).extracting("name").contains("market");
    }

    @Test
    void deniesNewWarpWhenLimitReachedButAllowsUpdatingExistingWarp() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.limits.setLimit(owner, "player_warp", 1);

        assertThat(fixture.service.setWarp(owner, "market", position(), false).status())
                .isEqualTo(PlayerWarpResult.Status.CREATED);
        assertThat(fixture.service.setWarp(owner, "farm", position(), false).status())
                .isEqualTo(PlayerWarpResult.Status.LIMIT_REACHED);
        assertThat(fixture.service.setWarp(owner, "market", movedPosition(), false).status())
                .isEqualTo(PlayerWarpResult.Status.UPDATED);
    }

    @Test
    void deniesWarpCreationOutsideTrustedClaim() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, false));

        PlayerWarpResult result = fixture.service.setWarp(UUID.randomUUID(), "market", position(), false);

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.CLAIM_DENIED);
        assertThat(result.messageKey()).isEqualTo("claim-denied");
    }

    @Test
    void resolvesVisiblePublicWarpAndDeletesOwnWarp() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);

        assertThat(fixture.service.resolveVisibleWarp(viewer, "market")).isPresent();
        assertThat(fixture.service.deleteWarp(owner, "market").status()).isEqualTo(PlayerWarpResult.Status.DELETED);
        assertThat(fixture.service.resolveVisibleWarp(viewer, "market")).isEmpty();
    }

    @Test
    void updatesWarpAccessAndVisibility() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);

        PlayerWarpResult accessResult = fixture.service.setAccess(owner, "market", AccessMode.TRUSTED);
        PlayerWarpResult visibilityResult = fixture.service.setVisibility(owner, "market", VisibilityMode.HIDDEN);

        assertThat(accessResult.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(visibilityResult.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(fixture.service.ownerWarps(owner).getFirst().accessMode()).isEqualTo(AccessMode.TRUSTED);
        assertThat(fixture.service.ownerWarps(owner).getFirst().visibilityMode()).isEqualTo(VisibilityMode.HIDDEN);
        assertThat(fixture.service.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.free());
    }

    @Test
    void ownerCanStillSeeHiddenPrivateWarpButStrangersCannot() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);
        fixture.service.setAccess(owner, "market", AccessMode.PRIVATE);
        fixture.service.setVisibility(owner, "market", VisibilityMode.HIDDEN);

        assertThat(fixture.service.visibleWarps(owner)).extracting("name").containsExactly("market");
        assertThat(fixture.service.visibleWarps(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatesWarpCost() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);

        PlayerWarpResult result = fixture.service.setCost(owner, "market", CostSpec.money(12.5));

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(fixture.service.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.money(12.5));
    }

    @Test
    void renamesWarpWithoutChangingSettings() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);
        fixture.service.setAccess(owner, "market", AccessMode.TRUSTED);
        fixture.service.setVisibility(owner, "market", VisibilityMode.HIDDEN);
        fixture.service.setCost(owner, "market", CostSpec.money(12.5));

        PlayerWarpResult result = fixture.service.rename(owner, "market", "bazaar");

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(fixture.service.resolveVisibleWarp(owner, "market")).isEmpty();
        assertThat(fixture.service.ownerWarps(owner).getFirst().name()).isEqualTo("bazaar");
        assertThat(fixture.service.ownerWarps(owner).getFirst().accessMode()).isEqualTo(AccessMode.TRUSTED);
        assertThat(fixture.service.ownerWarps(owner).getFirst().visibilityMode()).isEqualTo(VisibilityMode.HIDDEN);
        assertThat(fixture.service.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.money(12.5));
    }

    @Test
    void renameWarpRejectsMissingAndDuplicateNames() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);
        fixture.service.setWarp(owner, "bazaar", movedPosition(), false);

        PlayerWarpResult missing = fixture.service.rename(owner, "unknown", "new-name");
        PlayerWarpResult duplicate = fixture.service.rename(owner, "market", "bazaar");

        assertThat(missing.status()).isEqualTo(PlayerWarpResult.Status.NOT_FOUND);
        assertThat(duplicate.status()).isEqualTo(PlayerWarpResult.Status.DUPLICATE_NAME);
        assertThat(fixture.service.resolveVisibleWarp(owner, "market")).isPresent();
        assertThat(fixture.service.resolveVisibleWarp(owner, "bazaar").orElseThrow().position()).isEqualTo(movedPosition());
    }

    @Test
    void renameWarpAllowsSameNormalizedName() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);
        fixture.service.setCost(owner, "market", CostSpec.money(12.5));

        PlayerWarpResult result = fixture.service.rename(owner, "market", "Market");

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(fixture.service.resolveVisibleWarp(owner, "market")).isPresent();
        assertThat(fixture.service.ownerWarps(owner)).hasSize(1);
        assertThat(fixture.service.ownerWarps(owner).getFirst().name()).isEqualTo("Market");
        assertThat(fixture.service.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.money(12.5));
    }

    @Test
    void relocateWarpPreservesSettingsAndUsesCreationPolicy() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, true));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), false);
        fixture.service.setCost(owner, "market", CostSpec.xpLevels(5));

        PlayerWarpResult result = fixture.service.relocate(owner, "market", movedPosition(), false);

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.UPDATED);
        assertThat(fixture.service.ownerWarps(owner).getFirst().position()).isEqualTo(movedPosition());
        assertThat(fixture.service.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.xpLevels(5));
    }

    @Test
    void relocateWarpDeniesDestinationOutsideTrustedClaim() {
        Fixture fixture = Fixture.create(HavenClaimsGateway.fixed(true, false));
        UUID owner = UUID.randomUUID();
        fixture.service.setWarp(owner, "market", position(), true);

        PlayerWarpResult result = fixture.service.relocate(owner, "market", movedPosition(), false);

        assertThat(result.status()).isEqualTo(PlayerWarpResult.Status.CLAIM_DENIED);
    }

    private static SavedPosition position() {
        return new SavedPosition(WORLD_ID, "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(MOVED_WORLD_ID, "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private record Fixture(PlayerWarpService service, LimitService limits) {
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
            return new Fixture(new PlayerWarpService(locationService, limitService, creationPolicy), limitService);
        }
    }
}
