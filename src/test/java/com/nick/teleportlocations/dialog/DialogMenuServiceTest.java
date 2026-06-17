package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.LocationName;
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
    void homesMenuShowsTitleAndViewActionPerHome() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.homesMenu(owner, List.of(location(owner)), 5);

        assertThat(model.title()).isEqualTo("Homes");
        assertThat(model.actions()).extracting(DialogActionModel::key).contains("view:home:base");
    }

    @Test
    void homesMenuBodyShowsSlotCount() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        List<TeleportLocation> twoHomes = List.of(location(owner), location(owner, "home", "cave"));

        DialogMenuModel model = service.homesMenu(owner, twoHomes, 5);

        assertThat(model.lines()).contains("Homes (2/5)");
    }

    @Test
    void mainHomeHasStarPrefix() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        TeleportLocation main = mainHome(owner, "castle");

        DialogMenuModel model = service.homesMenu(owner, List.of(main), 5);

        assertThat(model.actions()).extracting(DialogActionModel::label)
                .anyMatch(label -> label.contains("★") && label.contains("castle"));
    }

    @Test
    void nonMainHomeHasNoStar() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        TeleportLocation home = nonMainHome(owner, "mine");

        DialogMenuModel model = service.homesMenu(owner, List.of(home), 5);

        assertThat(model.actions()).extracting(DialogActionModel::label)
                .filteredOn(label -> label.equals("mine"))
                .isNotEmpty();
        assertThat(model.actions()).extracting(DialogActionModel::label)
                .noneMatch(label -> label.contains("★") && label.contains("mine"));
    }

    @Test
    void addHomeButtonAlwaysPresent() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.homesMenu(owner, List.of(), 5);

        assertThat(model.actions()).extracting(DialogActionModel::label)
                .anyMatch(label -> label.contains("Add Home"));
    }

    @Test
    void addHomeButtonIsCapWhenFull() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        List<TeleportLocation> homes = List.of(
                mainHome(owner, "home-1"),
                nonMainHome(owner, "home-2"),
                nonMainHome(owner, "home-3")
        );

        DialogMenuModel model = service.homesMenu(owner, homes, 3);

        assertThat(model.actions()).extracting(DialogActionModel::key)
                .filteredOn(key -> key.equals("homes:cap"))
                .isNotEmpty();
    }

    @Test
    void addHomeButtonIsAddWhenNotFull() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();
        List<TeleportLocation> homes = List.of(
                mainHome(owner, "home-1"),
                nonMainHome(owner, "home-2")
        );

        DialogMenuModel model = service.homesMenu(owner, homes, 3);

        assertThat(model.actions()).extracting(DialogActionModel::key)
                .filteredOn(key -> key.equals("homes:add"))
                .isNotEmpty();
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
    void editMenuShowsShopSpecificActionsWithoutWarpSettings() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "shop"));

        assertThat(model.title()).isEqualTo("Edit Shop");
        assertThat(model.lines()).containsExactly("Name: base", "Location: world (0, 64, 0)");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "show-rename-menu:shop:base",
                "relocate:shop:base",
                "show-delete-confirm:shop:base"
        );
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly("Rename", "Relocate", "Delete");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .noneMatch(key -> key.contains("access") || key.contains("visibility") || key.contains("cost"));
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
    void editMenuShowsPlayerWarpHubActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "player_warp"));

        assertThat(model.lines()).containsExactly(
                "Name: base",
                "Access: Private",
                "Visibility: Hidden",
                "Cost: Free"
        );
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "show-access-menu:player_warp:base",
                "show-visibility-menu:player_warp:base",
                "show-cost-menu:player_warp:base",
                "show-rename-menu:player_warp:base",
                "relocate:player_warp:base",
                "show-delete-confirm:player_warp:base"
        );
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly(
                "Access",
                "Visibility",
                "Cost",
                "Rename",
                "Relocate",
                "Delete"
        );
    }

    @Test
    void accessMenuIncludesPlayerWarpAccessActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.accessMenu(location(owner, "player_warp"));

        assertThat(model.title()).isEqualTo("Access");
        assertThat(model.lines()).containsExactly("Name: base", "Access: Private");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "set-access:player_warp:base:public",
                "set-access:player_warp:base:trusted",
                "set-access:player_warp:base:private"
        );
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly("Public", "Trusted", "Private");
    }

    @Test
    void visibilityMenuIncludesPlayerWarpVisibilityActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.visibilityMenu(location(owner, "player_warp"));

        assertThat(model.title()).isEqualTo("Visibility");
        assertThat(model.lines()).containsExactly("Name: base", "Visibility: Hidden");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "set-visibility:player_warp:base:listed",
                "set-visibility:player_warp:base:unlisted",
                "set-visibility:player_warp:base:hidden"
        );
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly("Listed", "Unlisted", "Hidden");
    }

    @Test
    void costMenuIncludesPlayerWarpPresetAndCustomCostActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.costMenu(location(owner, "player_warp"));

        assertThat(model.title()).isEqualTo("Cost");
        assertThat(model.lines()).containsExactly("Name: base", "Cost: Free");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
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
                "show-cost-editor:player_warp:base:xp-points"
        );
    }

    @Test
    void renameMenuIncludesNameInputAndSubmitAction() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.renameMenu(location(owner, "player_warp"));

        assertThat(model.title()).isEqualTo("Rename Player Warp");
        assertThat(model.lines()).containsExactly("Name: base");
        assertThat(model.actions()).extracting(DialogActionModel::key)
                .containsExactly("rename-input:player_warp:base");
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly("Rename");
        assertThat(model.inputs()).containsExactly(DialogInputModel.text("name", "Name", "base", 32));
    }

    @Test
    void deleteConfirmMenuIncludesConfirmAndCancelActions() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.deleteConfirmMenu(location(owner, "shop"));

        assertThat(model.title()).isEqualTo("Delete Shop");
        assertThat(model.lines()).containsExactly("Name: base");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly(
                "confirm-delete:shop:base",
                "cancel-delete:shop:base"
        );
        assertThat(model.actions()).extracting(DialogActionModel::label).containsExactly("Confirm Delete", "Cancel");
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
        return location(owner, category, "base");
    }

    private static TeleportLocation location(UUID owner, String category, String name) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                category,
                OwnerRef.player(owner),
                name,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                "home".equals(category),
                Instant.EPOCH
        );
    }

    private static TeleportLocation mainHome(UUID owner, String name) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                "home",
                OwnerRef.player(owner),
                name,
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                true,
                Instant.EPOCH
        );
    }

    private static TeleportLocation nonMainHome(UUID owner, String name) {
        return new TeleportLocation(
                UUID.randomUUID(),
                "home",
                OwnerRef.player(owner),
                name,
                LocationName.normalize(name),
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                false,
                Instant.EPOCH,
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
