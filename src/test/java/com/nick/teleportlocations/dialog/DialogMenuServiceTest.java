package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
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
    void shopWarpsMenuIncludesTeleportAndOwnerEditAction() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.shopWarpsMenu(owner, List.of(location(owner, "shop")));

        assertThat(model.title()).isEqualTo("Shop Warps");
        assertThat(model.lines()).contains("Shop: base");
        assertThat(model.actions()).extracting(DialogActionModel::key).contains("teleport:shop:base", "edit:shop:base");
    }

    @Test
    void editMenuShowsShopInvariantAsFixedState() {
        UUID owner = UUID.randomUUID();
        DialogMenuService service = new DialogMenuService();

        DialogMenuModel model = service.editMenu(location(owner, "shop"));

        assertThat(model.title()).isEqualTo("Edit Shop");
        assertThat(model.lines()).contains("Access: Public", "Visibility: Listed", "Cost: Free");
        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly("delete:shop:base");
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

        assertThat(model.actions()).extracting(DialogActionModel::key).containsExactly("delete:player_warp:base");
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
}
