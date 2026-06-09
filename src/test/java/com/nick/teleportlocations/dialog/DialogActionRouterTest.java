package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.elevator.ElevatorService;
import com.nick.teleportlocations.elevator.InMemoryElevatorRepository;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import com.nick.teleportlocations.teleportblock.InMemoryTeleportBlockRepository;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import com.nick.teleportlocations.teleportblock.TeleportBlockService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DialogActionRouterTest {
    @Test
    void routesHomeTeleportActionToResolvedLocation() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.homes.setHome(owner, "base", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "teleport:home:base");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.TELEPORT);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().name()).isEqualTo("base");
    }

    @Test
    void routesShopEditActionToOwnerOnlyEditMenu() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult ownerResult = fixture.router.route(owner, "edit:shop:tools");
        DialogActionRouteResult strangerResult = fixture.router.route(UUID.randomUUID(), "edit:shop:tools");

        assertThat(ownerResult.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(ownerResult.menu()).isPresent();
        assertThat(ownerResult.menu().orElseThrow().title()).isEqualTo("Edit Shop");
        assertThat(strangerResult.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
    }

    @Test
    void rejectsUnknownDialogAction() {
        Fixture fixture = Fixture.create();

        DialogActionRouteResult result = fixture.router.route(UUID.randomUUID(), "dance:shop:tools");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
    }

    @Test
    void routesServerWarpTeleportActionToResolvedLocation() {
        Fixture fixture = Fixture.create();
        fixture.serverWarps.setWarp("spawn", position());

        DialogActionRouteResult result = fixture.router.route(UUID.randomUUID(), "teleport:server_warp:spawn");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.TELEPORT);
        assertThat(result.location()).isPresent();
        assertThat(result.location().orElseThrow().name()).isEqualTo("spawn");
    }

    @Test
    void setMainHomeActionUpdatesTheSelectedHome() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.homes.setHome(owner, "base", position(), true);
        fixture.homes.setHome(owner, "vault", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "set-main:home:vault");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.homes.resolveHome(owner, "").orElseThrow().name()).isEqualTo("vault");
    }

    @Test
    void deleteShopActionRemovesOwnedShop() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "delete:shop:tools");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isEmpty();
    }

    @Test
    void playerWarpEditActionsUpdateAccessAndVisibilityForOwner() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult accessResult = fixture.router.route(owner, "set-access:player_warp:market:trusted");
        DialogActionRouteResult visibilityResult = fixture.router.route(owner, "set-visibility:player_warp:market:hidden");

        assertThat(accessResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(visibilityResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().accessMode()).isEqualTo(AccessMode.TRUSTED);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().visibilityMode()).isEqualTo(VisibilityMode.HIDDEN);
    }

    @Test
    void shopEditActionsCannotChangeAccessOrVisibility() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult accessResult = fixture.router.route(owner, "set-access:shop:tools:private");
        DialogActionRouteResult visibilityResult = fixture.router.route(owner, "set-visibility:shop:tools:hidden");

        assertThat(accessResult.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(visibilityResult.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools").orElseThrow().accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools").orElseThrow().visibilityMode()).isEqualTo(VisibilityMode.LISTED);
    }

    @Test
    void playerWarpCostActionsUpdateCostForOwner() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult moneyResult = fixture.router.route(owner, "set-cost:player_warp:market:money:50");
        assertThat(moneyResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.money(50.0));

        DialogActionRouteResult xpResult = fixture.router.route(owner, "set-cost:player_warp:market:xp-levels:10");
        assertThat(xpResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.xpLevels(10));

        DialogActionRouteResult freeResult = fixture.router.route(owner, "set-cost:player_warp:market:free:0");
        assertThat(freeResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.free());
    }

    @Test
    void shopCostActionsRemainUnavailable() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "set-cost:shop:tools:money:50");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools").orElseThrow().cost()).isEqualTo(CostSpec.free());
    }

    @Test
    void customCostEditorActionShowsInputMenuForOwner() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "show-cost-editor:player_warp:market:money");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(result.menu()).isPresent();
        assertThat(result.menu().orElseThrow().inputs()).extracting(DialogInputModel::key).containsExactly("amount");
    }

    @Test
    void customCostInputActionUpdatesCostFromDialogAmount() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult result = fixture.router.route(
                owner,
                "set-cost-input:player_warp:market:money",
                key -> "amount".equals(key) ? 72.5f : null
        );

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.warps.ownerWarps(owner).getFirst().cost()).isEqualTo(CostSpec.money(72.5));
    }

    @Test
    void customCostInputRejectsShopAndMissingAmount() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult shopResult = fixture.router.route(
                owner,
                "set-cost-input:shop:tools:money",
                key -> "amount".equals(key) ? 50.0f : null
        );
        DialogActionRouteResult missingAmountResult = fixture.router.route(
                owner,
                "set-cost-input:player_warp:market:money",
                key -> null
        );

        assertThat(shopResult.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(missingAmountResult.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools").orElseThrow().cost()).isEqualTo(CostSpec.free());
    }

    @Test
    void elevatorParticleActionUpdatesForOwnerWithParticlePermission() {
        Fixture fixture = Fixture.create(Set.of("teleportlocations.elevator.particle.end_rod"));
        UUID owner = UUID.randomUUID();
        ElevatorBlock block = fixture.elevators.place(owner, position(), false).block().orElseThrow();

        DialogActionRouteResult result = fixture.router.route(owner, "set-elevator-particle:" + block.id() + ":end_rod");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.elevators.findAt(block.position()).orElseThrow().particle()).isEqualTo(ElevatorParticle.END_ROD);
    }

    @Test
    void elevatorParticleActionRequiresOwnerOrAdminAndParticlePermission() {
        UUID visitor = UUID.randomUUID();
        Fixture fixture = Fixture.create(Set.of("teleportlocations.elevator.particle.end_rod"));
        ElevatorBlock block = fixture.elevators.place(UUID.randomUUID(), position(), false).block().orElseThrow();
        Fixture noPermissionFixture = Fixture.create(Set.of());
        ElevatorBlock noPermissionBlock = noPermissionFixture.elevators.place(UUID.randomUUID(), position(), false).block().orElseThrow();

        DialogActionRouteResult visitorResult = fixture.router.route(visitor, "set-elevator-particle:" + block.id() + ":end_rod");
        DialogActionRouteResult noPermissionResult = noPermissionFixture.router.route(
                noPermissionBlock.ownerId(),
                "set-elevator-particle:" + noPermissionBlock.id() + ":end_rod"
        );

        assertThat(visitorResult.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
        assertThat(noPermissionResult.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
    }

    @Test
    void elevatorAdminParticleOverrideRequiresClaimBypassMode() {
        UUID owner = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        Fixture fixture = Fixture.create(Set.of(
                "teleportlocations.admin.elevator",
                "teleportlocations.elevator.particle.end_rod"
        ));
        ElevatorBlock block = fixture.elevators.place(owner, position(), false).block().orElseThrow();

        DialogActionRouteResult withoutBypass = fixture.router.route(admin, "set-elevator-particle:" + block.id() + ":end_rod");
        fixture.bypass.setClaims(admin, true);
        DialogActionRouteResult withBypass = fixture.router.route(admin, "set-elevator-particle:" + block.id() + ":end_rod");

        assertThat(withoutBypass.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
        assertThat(withBypass.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
    }

    @Test
    void teleportBlockTargetActionSetsOwnedLocation() {
        Fixture fixture = Fixture.create(Set.of("teleportlocations.teleportblock.link"));
        UUID owner = UUID.randomUUID();
        TeleportBlock block = fixture.teleportBlocks.place(owner, position(), false).block().orElseThrow();
        fixture.homes.setHome(owner, "base", position(), true);
        UUID homeId = fixture.homes.resolveHome(owner, "base").orElseThrow().id();

        DialogActionRouteResult result = fixture.router.route(owner, "set-teleport-block-target:" + block.id() + ":" + homeId);

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.teleportBlocks.findAt(block.position()).orElseThrow().targetLocationId()).contains(homeId);
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private record Fixture(
            DialogActionRouter router,
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            ServerWarpService serverWarps,
            ElevatorService elevators,
            TeleportBlockService teleportBlocks,
            AdminBypassService bypass
    ) {
        private static Fixture create() {
            return create(Set.of());
        }

        private static Fixture create(Set<String> permissions) {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    config.categories(),
                    LandClaimsGateway.fixed(false, true),
                    MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            HomeService homeService = new HomeService(locationService, limitService, creationPolicy);
            PlayerWarpService warpService = new PlayerWarpService(locationService, limitService, creationPolicy);
            ShopWarpService shopService = new ShopWarpService(locationService, limitService, creationPolicy);
            OutpostService outpostService = new OutpostService(locationService, limitService, creationPolicy);
            ServerWarpService serverWarpService = new ServerWarpService(locationService);
            AdminBypassService bypassService = new AdminBypassService();
            ElevatorService elevatorService = new ElevatorService(
                    new InMemoryElevatorRepository(),
                    LandClaimsGateway.fixedOwned(true, true, true),
                    () -> Instant.EPOCH
            );
            TeleportBlockService teleportBlockService = new TeleportBlockService(
                    new InMemoryTeleportBlockRepository(),
                    LandClaimsGateway.fixedOwned(true, true, true),
                    () -> Instant.EPOCH
            );
            DialogMenuService menus = new DialogMenuService();
            return new Fixture(
                    new DialogActionRouter(
                            homeService,
                            warpService,
                            shopService,
                            outpostService,
                            serverWarpService,
                            elevatorService,
                            teleportBlockService,
                            locationService,
                            menus,
                            bypassService,
                            (viewer, permission) -> permissions.contains(permission)
                    ),
                    homeService,
                    warpService,
                    shopService,
                    outpostService,
                    serverWarpService,
                    elevatorService,
                    teleportBlockService,
                    bypassService
            );
        }
    }
}
