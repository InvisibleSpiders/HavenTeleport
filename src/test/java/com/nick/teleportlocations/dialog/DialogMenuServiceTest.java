package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DialogMenuServiceTest {
    @Test
    void homesMenuIncludesTeleportAndEditForOwner() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.homesMenu(owner, List.of(location(owner)));

        assertThat(model.title()).isEqualTo("Homes");
        assertThat(model.actions()).extracting(DialogActionModel::key).contains("teleport:home:base", "edit:home:base");
    }

    @Test
    void playerWarpsMenuIncludesTeleportAndOwnerEditAction() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.playerWarpsMenu(owner, List.of(location(owner, "player_warp")));

        assertThat(model.title()).isEqualTo("Player Warps");
        assertThat(model.lines()).contains("Warp: base");
        assertThat(model.actions()).extracting(DialogActionModel::key).contains("teleport:player_warp:base", "edit:player_warp:base");
    }

    @Test
    void warpsMenuIncludesServerAndPlayerWarpActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.warpsMenu(owner, List.of(serverLocation()), List.of(location(owner, "player_warp")));

        assertThat(model.title()).isEqualTo("Warps");
        assertThat(model.lines()).contains("Server: spawn", "Player: base");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .contains("teleport:server_warp:spawn", "teleport:player_warp:base", "edit:player_warp:base");
    }

    @Test
    void warpsMenuMarksInaccessiblePlayerWarpAndOmitsTeleportAction() {
        UUID viewer = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.warpsMenu(
                viewer,
                List.of(),
                List.of(location(UUID.randomUUID(), "player_warp")),
                location -> false,
                false
        );

        assertThat(model.lines()).contains("Player: base (No claim access)");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .doesNotContain("teleport:player_warp:base");
    }

    @Test
    void warpsMenuCanHideInaccessiblePlayerWarp() {
        UUID viewer = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.warpsMenu(
                viewer,
                List.of(),
                List.of(location(UUID.randomUUID(), "player_warp")),
                location -> false,
                true
        );

        assertThat(model.lines()).isEmpty();
        assertThat(model.actions()).isEmpty();
    }

    @Test
    void shopWarpsMenuIncludesTeleportAndOwnerEditAction() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.shopWarpsMenu(owner, List.of(location(owner, "shop")));

        assertThat(model.title()).isEqualTo("Shop Warps");
        assertThat(model.lines()).contains("Shop: base");
        assertThat(model.actions()).extracting(DialogActionModel::key).contains("teleport:shop:base", "edit:shop:base");
    }

    @Test
    void shopWarpsMenuMarksInaccessibleShopAndOmitsTeleportAction() {
        UUID viewer = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.shopWarpsMenu(
                viewer,
                List.of(location(UUID.randomUUID(), "shop")),
                location -> false,
                false
        );

        assertThat(model.lines()).contains("Shop: base (No claim access)");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .doesNotContain("teleport:shop:base");
    }

    @Test
    void editMenuShowsShopInvariantAsFixedState() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "shop"));

        assertThat(model.title()).isEqualTo("Edit Shop");
        assertThat(model.lines()).contains("Access: Public", "Visibility: Listed", "Cost: Free");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly("delete:shop:base");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .noneMatch(key -> key.startsWith("set-access:") || key.startsWith("set-visibility:"));
    }

    @Test
    void editMenuIncludesMainHomeAndDeleteActionsForHomes() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "home"));

        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("set-main:home:base", "delete:home:base");
    }

    @Test
    void editMenuIncludesDeleteActionForPlayerWarps() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "player_warp"));

        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "set-access:player_warp:base:public",
                "set-access:player_warp:base:trusted",
                "set-access:player_warp:base:private",
                "set-visibility:player_warp:base:listed",
                "set-visibility:player_warp:base:unlisted",
                "set-visibility:player_warp:base:hidden",
                "set-cost:player_warp:base:free:0",
                "set-cost:player_warp:base:money:10",
                "set-cost:player_warp:base:money:50",
                "set-cost:player_warp:base:money:100",
                "set-cost:player_warp:base:xp-levels:5",
                "set-cost:player_warp:base:xp-levels:10",
                "set-cost:player_warp:base:xp-points:100",
                "set-cost:player_warp:base:xp-points:500",
                "show-cost-editor:player_warp:base:money",
                "show-cost-editor:player_warp:base:xp-levels",
                "show-cost-editor:player_warp:base:xp-points",
                "delete:player_warp:base"
        );
    }

    @Test
    void customCostMenuIncludesAmountInputAndSubmitAction() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.customCostMenu(location(owner, "player_warp"), "money");

        assertThat(model.title()).isEqualTo("Custom Money Cost");
        assertThat(model.inputs()).extracting(DialogInputModel::key).containsExactly("amount");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("set-cost-input:player_warp:base:money");
    }

    @Test
    void elevatorSettingsMenuShowsOnlyEditableAllowedParticles() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        ElevatorBlock block = elevator(owner, ElevatorParticle.WAX_ON);

        DialogMenuModel model = service.elevatorSettingsMenu(block, true, particle -> particle == ElevatorParticle.WAX_ON);

        assertThat(model.title()).isEqualTo("Elevator Settings");
        assertThat(model.lines()).contains("Particle: Wax On");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("set-elevator-particle:" + block.id() + ":wax_on");
    }

    @Test
    void elevatorSettingsMenuIsReadOnlyWithoutEditAccess() {
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.elevatorSettingsMenu(elevator(UUID.randomUUID(), ElevatorParticle.END_ROD), false, particle -> true);

        assertThat(model.lines()).contains("Particle: End Rod");
        assertThat(model.actions()).isEmpty();
    }

    @Test
    void teleportBlockSettingsMenuListsEditableTargets() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        TeleportBlock block = teleportBlock(owner);
        TeleportLocation home = location(owner, "home");
        TeleportLocation server = serverLocation();

        DialogMenuModel model = service.teleportBlockSettingsMenu(block, List.of(home), List.of(server), true);

        assertThat(model.title()).isEqualTo("Teleport Block");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly(
                        "set-teleport-block-target:" + block.id() + ":" + home.id(),
                        "set-teleport-block-target:" + block.id() + ":" + server.id()
                );
    }

    @Test
    void adminMenuShowsBypassStateAndServerWarpAction() {
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.adminMenu(true);

        assertThat(model.title()).isEqualTo("HavenTeleport Admin");
        assertThat(model.lines()).contains("Claim Bypass: Enabled");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("admin-toggle-claims-bypass", "admin-show-server-warps");
    }

    @Test
    void adminServerWarpsMenuListsConfiguredWarps() {
        DialogMenuService service = new DialogMenuService();
        TeleportLocation server = serverLocation();

        DialogMenuModel model = service.adminServerWarpsMenu(List.of(server));

        assertThat(model.title()).isEqualTo("Server Warps");
        assertThat(model.lines()).contains("Warp: spawn");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("teleport:server_warp:spawn");
    }

    private static TeleportLocation location(UUID owner) {
        return location(owner, "home");
    }

    private static TeleportLocation location(UUID owner, String category) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                category,
                OwnerRef.player(owner),
                "base",
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                "home".equals(category),
                Instant.EPOCH
        );
    }

    private static TeleportLocation serverLocation() {
        return TeleportLocation.create(
                UUID.randomUUID(),
                "server_warp",
                OwnerRef.server(),
                "spawn",
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                false,
                Instant.EPOCH
        );
    }

    private static ElevatorBlock elevator(UUID owner, ElevatorParticle particle) {
        return new ElevatorBlock(
                UUID.randomUUID(),
                owner,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                particle,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private static TeleportBlock teleportBlock(UUID owner) {
        return new TeleportBlock(
                UUID.randomUUID(),
                owner,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                Instant.EPOCH,
                Instant.EPOCH
        );
    }
}
