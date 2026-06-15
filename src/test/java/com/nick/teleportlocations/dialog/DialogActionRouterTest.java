package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.claim.MissingHavenClaimsPolicy;
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
import com.nick.teleportlocations.teleport.ManagedTeleportService;
import com.nick.teleportlocations.teleport.ScheduledTeleportService;
import com.nick.teleportlocations.teleport.TeleportAccessService;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import com.nick.teleportlocations.teleport.TeleportSafetyService;
import com.nick.teleportlocations.teleportblock.InMemoryTeleportBlockRepository;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import com.nick.teleportlocations.teleportblock.TeleportBlockService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void showDeleteConfirmActionDoesNotDeleteAndReturnsConfirmAndCancelActions() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "show-delete-confirm:shop:tools");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(result.menu()).isPresent();
        assertThat(result.menu().orElseThrow().title()).isEqualTo("Delete Shop");
        assertThat(actionKeys(result.menu().orElseThrow())).containsExactly(
                "confirm-delete:shop:tools",
                "cancel-delete:shop:tools"
        );
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isPresent();
    }

    @Test
    void cancelDeleteActionDoesNotDeleteAndReturnsEditMenu() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "cancel-delete:shop:tools");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(result.menu()).isPresent();
        assertThat(result.menu().orElseThrow().title()).isEqualTo("Edit Shop");
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isPresent();
    }

    @Test
    void confirmDeleteActionRemovesOwnedShop() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "confirm-delete:shop:tools");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isEmpty();
    }

    @Test
    void malformedConfirmDeleteActionDoesNotDelete() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(owner, "confirm-delete:shop:tools:anything");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isPresent();
    }

    @Test
    void malformedRenameInputActionDoesNotRename() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult result = fixture.router.route(
                owner,
                "rename-input:shop:tools:anything",
                textInput("name", "gear")
        );

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isPresent();
        assertThat(fixture.shops.resolveVisibleShop(owner, "gear")).isEmpty();
    }

    @Test
    void malformedRelocateActionDoesNotMovePlayerWarp() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        SavedPosition original = position();
        fixture.warps.setWarp(owner, "market", original, true);

        DialogActionRouteResult result = fixture.router.route(
                owner,
                "relocate:player_warp:market:anything",
                DialogInputValues.empty(),
                movedPosition(),
                false
        );

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(fixture.warps.resolveVisibleWarp(owner, "market").orElseThrow().position()).isEqualTo(original);
    }

    @Test
    void nonOwnerDeleteConfirmationActionsDoNotDeleteTarget() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        UUID visitor = UUID.randomUUID();
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult show = fixture.router.route(visitor, "show-delete-confirm:shop:tools");
        DialogActionRouteResult cancel = fixture.router.route(visitor, "cancel-delete:shop:tools");
        DialogActionRouteResult confirm = fixture.router.route(visitor, "confirm-delete:shop:tools");

        assertThat(show.status()).isEqualTo(DialogActionRouteResult.Status.NOT_FOUND);
        assertThat(cancel.status()).isEqualTo(DialogActionRouteResult.Status.NOT_FOUND);
        assertThat(confirm.status()).isEqualTo(DialogActionRouteResult.Status.NOT_FOUND);
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isPresent();
    }

    @Test
    void playerWarpSubmenuActionsReturnTheirMenus() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult access = fixture.router.route(owner, "show-access-menu:player_warp:market");
        DialogActionRouteResult visibility = fixture.router.route(owner, "show-visibility-menu:player_warp:market");
        DialogActionRouteResult cost = fixture.router.route(owner, "show-cost-menu:player_warp:market");
        DialogActionRouteResult rename = fixture.router.route(owner, "show-rename-menu:player_warp:market");

        assertThat(access.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(access.menu().orElseThrow().title()).isEqualTo("Access");
        assertThat(visibility.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(visibility.menu().orElseThrow().title()).isEqualTo("Visibility");
        assertThat(cost.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(cost.menu().orElseThrow().title()).isEqualTo("Cost");
        assertThat(rename.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(rename.menu().orElseThrow().title()).isEqualTo("Rename Player Warp");
    }

    @Test
    void renameInputRenamesPlayerWarpAndShopFromTextInput() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.warps.setWarp(owner, "market", position(), true);
        fixture.shops.setShop(owner, "tools", position(), true);

        DialogActionRouteResult warpResult = fixture.router.route(owner, "rename-input:player_warp:market", textInput("name", "bazaar"));
        DialogActionRouteResult shopResult = fixture.router.route(owner, "rename-input:shop:tools", textInput("name", "gear"));

        assertThat(warpResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(warpResult.message()).isEqualTo("Warp renamed to bazaar.");
        assertThat(shopResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(shopResult.message()).isEqualTo("Shop renamed to gear.");
        assertThat(fixture.warps.resolveVisibleWarp(owner, "bazaar")).isPresent();
        assertThat(fixture.warps.resolveVisibleWarp(owner, "market")).isEmpty();
        assertThat(fixture.shops.resolveVisibleShop(owner, "gear")).isPresent();
        assertThat(fixture.shops.resolveVisibleShop(owner, "tools")).isEmpty();
    }

    @Test
    void renameInputReportsDuplicateAndInvalidNames() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        fixture.limits.setLimit(owner, "player_warp", 2);
        fixture.limits.setLimit(owner, "shop", 2);
        fixture.warps.setWarp(owner, "market", position(), true);
        fixture.warps.setWarp(owner, "bazaar", position(), true);
        fixture.shops.setShop(owner, "tools", position(), true);
        fixture.shops.setShop(owner, "gear", position(), true);

        DialogActionRouteResult duplicateWarp = fixture.router.route(owner, "rename-input:player_warp:market", textInput("name", "bazaar"));
        DialogActionRouteResult invalidWarp = fixture.router.route(owner, "rename-input:player_warp:market", textInput("name", "market:west"));
        DialogActionRouteResult duplicateShop = fixture.router.route(owner, "rename-input:shop:tools", textInput("name", "gear"));
        DialogActionRouteResult invalidShop = fixture.router.route(owner, "rename-input:shop:tools", textInput("name", "tools:west"));

        assertThat(duplicateWarp.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(duplicateWarp.message()).isEqualTo("That warp name is already in use.");
        assertThat(invalidWarp.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(invalidWarp.message()).isEqualTo("That warp name is invalid.");
        assertThat(duplicateShop.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(duplicateShop.message()).isEqualTo("That shop name is already in use.");
        assertThat(invalidShop.status()).isEqualTo(DialogActionRouteResult.Status.UNKNOWN_ACTION);
        assertThat(invalidShop.message()).isEqualTo("That shop name is invalid.");
    }

    @Test
    void relocatePlayerWarpUsesProvidedCurrentPosition() {
        Fixture fixture = Fixture.create();
        UUID owner = UUID.randomUUID();
        SavedPosition moved = movedPosition();
        fixture.warps.setWarp(owner, "market", position(), true);

        DialogActionRouteResult result = fixture.router.route(
                owner,
                "relocate:player_warp:market",
                DialogInputValues.empty(),
                moved,
                false
        );

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(result.message()).isEqualTo("Warp market relocated.");
        assertThat(fixture.warps.resolveVisibleWarp(owner, "market").orElseThrow().position()).isEqualTo(moved);
    }

    @Test
    void relocateShopRequiresOwnedPublicClaimPolicy() {
        UUID owner = UUID.randomUUID();
        SavedPosition original = position();
        SavedPosition moved = movedPosition();
        Fixture allowed = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, true));
        allowed.shops.setShop(owner, "tools", original, true);
        Fixture denied = Fixture.create(HavenClaimsGateway.fixedOwned(true, true, false));
        denied.shops.setShop(owner, "tools", original, true);

        DialogActionRouteResult allowedResult = allowed.router.route(
                owner,
                "relocate:shop:tools",
                DialogInputValues.empty(),
                moved,
                false
        );
        DialogActionRouteResult deniedResult = denied.router.route(
                owner,
                "relocate:shop:tools",
                DialogInputValues.empty(),
                moved,
                false
        );

        assertThat(allowedResult.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(allowed.shops.resolveVisibleShop(owner, "tools").orElseThrow().position()).isEqualTo(moved);
        assertThat(deniedResult.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
        assertThat(denied.shops.resolveVisibleShop(owner, "tools").orElseThrow().position()).isEqualTo(original);
    }

    @Test
    void executorRoutesWithPlayerCurrentPositionAndCreationBypassPermission() {
        DialogActionRouter router = mock(DialogActionRouter.class);
        PaperDialogPresenter presenter = mock(PaperDialogPresenter.class);
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        UUID playerId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(location);
        when(player.hasPermission("teleportlocations.admin.bypass.creation")).thenReturn(true);
        when(location.getWorld()).thenReturn(world);
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");
        when(location.getX()).thenReturn(4.0);
        when(location.getY()).thenReturn(70.0);
        when(location.getZ()).thenReturn(5.0);
        when(location.getYaw()).thenReturn(180.0f);
        when(location.getPitch()).thenReturn(10.0f);
        DialogInputValues inputValues = DialogInputValues.empty();
        when(router.route(
                org.mockito.Mockito.eq(playerId),
                org.mockito.Mockito.eq("relocate:player_warp:market"),
                org.mockito.Mockito.eq(inputValues),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.eq(true)
        )).thenReturn(DialogActionRouteResult.unknownAction());
        DialogActionExecutor executor = new DialogActionExecutor(
                router,
                presenter,
                mock(TeleportChargeService.class),
                mock(TeleportAccessService.class),
                mock(TeleportSafetyService.class),
                new AdminBypassService(),
                mock(ManagedTeleportService.class),
                mock(ScheduledTeleportService.class)
        );

        executor.handle(player, "relocate:player_warp:market", inputValues);

        ArgumentCaptor<SavedPosition> position = ArgumentCaptor.forClass(SavedPosition.class);
        verify(router).route(
                org.mockito.Mockito.eq(playerId),
                org.mockito.Mockito.eq("relocate:player_warp:market"),
                org.mockito.Mockito.eq(inputValues),
                position.capture(),
                org.mockito.Mockito.eq(true)
        );
        assertThat(position.getValue()).isEqualTo(BukkitLocations.save(location));
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

    @Test
    void adminToggleClaimsBypassActionRequiresPermission() {
        UUID admin = UUID.randomUUID();
        Fixture fixture = Fixture.create(Set.of("teleportlocations.admin.bypass.claims"));
        Fixture denied = Fixture.create(Set.of());

        DialogActionRouteResult deniedResult = denied.router.route(admin, "admin-toggle-claims-bypass");
        DialogActionRouteResult result = fixture.router.route(admin, "admin-toggle-claims-bypass");

        assertThat(deniedResult.status()).isEqualTo(DialogActionRouteResult.Status.ACCESS_DENIED);
        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.MESSAGE);
        assertThat(fixture.bypass.claims(admin)).isTrue();
    }

    @Test
    void adminServerWarpsActionShowsServerWarpMenu() {
        UUID admin = UUID.randomUUID();
        Fixture fixture = Fixture.create(Set.of("teleportlocations.admin.serverwarp"));
        fixture.serverWarps.setWarp("spawn", position());

        DialogActionRouteResult result = fixture.router.route(admin, "admin-show-server-warps");

        assertThat(result.status()).isEqualTo(DialogActionRouteResult.Status.SHOW_MENU);
        assertThat(result.menu()).isPresent();
        assertThat(result.menu().orElseThrow().title()).isEqualTo("Server Warps");
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }

    private static SavedPosition movedPosition() {
        return new SavedPosition(UUID.randomUUID(), "world", 4.0, 70.0, 5.0, 180.0f, 10.0f);
    }

    private static List<String> actionKeys(DialogMenuModel menu) {
        return menu.actions().stream().map(DialogActionModel::key).toList();
    }

    private static DialogInputValues textInput(String inputKey, String value) {
        return new DialogInputValues() {
            @Override
            public Float getFloat(String key) {
                return null;
            }

            @Override
            public String getText(String key) {
                return inputKey.equals(key) ? value : null;
            }
        };
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
            LimitService limits,
            AdminBypassService bypass
    ) {
        private static Fixture create() {
            return create(Set.of());
        }

        private static Fixture create(Set<String> permissions) {
            return create(permissions, HavenClaimsGateway.fixed(false, true));
        }

        private static Fixture create(HavenClaimsGateway havenClaims) {
            return create(Set.of(), havenClaims);
        }

        private static Fixture create(Set<String> permissions, HavenClaimsGateway havenClaims) {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    config.categories(),
                    havenClaims,
                    MissingHavenClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            HomeService homeService = new HomeService(locationService, limitService, creationPolicy);
            PlayerWarpService warpService = new PlayerWarpService(locationService, limitService, creationPolicy);
            ShopWarpService shopService = new ShopWarpService(locationService, limitService, creationPolicy);
            OutpostService outpostService = new OutpostService(locationService, limitService, creationPolicy);
            ServerWarpService serverWarpService = new ServerWarpService(locationService);
            AdminBypassService bypassService = new AdminBypassService();
            ElevatorService elevatorService = new ElevatorService(
                    new InMemoryElevatorRepository(),
                    HavenClaimsGateway.fixedOwned(true, true, true),
                    () -> Instant.EPOCH
            );
            TeleportBlockService teleportBlockService = new TeleportBlockService(
                    new InMemoryTeleportBlockRepository(),
                    HavenClaimsGateway.fixedOwned(true, true, true),
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
                    limitService,
                    bypassService
            );
        }
    }
}
