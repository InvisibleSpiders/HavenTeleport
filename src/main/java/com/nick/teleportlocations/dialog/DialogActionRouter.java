package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.home.HomeResult;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.outpost.OutpostResult;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.shop.ShopWarpResult;
import com.nick.teleportlocations.warp.PlayerWarpService;
import com.nick.teleportlocations.warp.PlayerWarpResult;
import java.util.Optional;
import java.util.UUID;

public final class DialogActionRouter {
    private final HomeService homes;
    private final PlayerWarpService warps;
    private final ShopWarpService shops;
    private final OutpostService outposts;
    private final DialogMenuService menus;

    public DialogActionRouter(
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            DialogMenuService menus
    ) {
        this.homes = homes;
        this.warps = warps;
        this.shops = shops;
        this.outposts = outposts;
        this.menus = menus;
    }

    public DialogActionRouteResult route(UUID viewerId, String actionKey) {
        String[] parts = actionKey.split(":", 3);
        if (parts.length != 3) {
            return DialogActionRouteResult.unknownAction();
        }
        return switch (parts[0]) {
            case "teleport", "edit" -> routeLocationAction(viewerId, parts[0], parts[1], parts[2]);
            case "set-main" -> setMainHome(viewerId, parts[1], parts[2]);
            case "delete" -> deleteLocation(viewerId, parts[1], parts[2]);
            default -> DialogActionRouteResult.unknownAction();
        };
    }

    private DialogActionRouteResult routeLocationAction(UUID viewerId, String action, String category, String name) {
        Optional<TeleportLocation> location = resolve(viewerId, category, name);
        if (location.isEmpty()) {
            return DialogActionRouteResult.notFound();
        }

        TeleportLocation resolved = location.orElseThrow();
        return switch (action) {
            case "teleport" -> DialogActionRouteResult.teleport(resolved);
            case "edit" -> resolved.owner().playerIdOptional().filter(viewerId::equals).isPresent()
                    ? DialogActionRouteResult.showMenu(menus.editMenu(resolved))
                    : DialogActionRouteResult.accessDenied();
            default -> DialogActionRouteResult.unknownAction();
        };
    }

    private DialogActionRouteResult setMainHome(UUID viewerId, String category, String name) {
        if (!"home".equals(category)) {
            return DialogActionRouteResult.unknownAction();
        }
        HomeResult result = homes.setMainHome(viewerId, name);
        return switch (result.status()) {
            case UPDATED -> DialogActionRouteResult.message("Main home set to " + name + ".");
            case NOT_FOUND -> DialogActionRouteResult.notFound();
            default -> DialogActionRouteResult.unknownAction();
        };
    }

    private DialogActionRouteResult deleteLocation(UUID viewerId, String category, String name) {
        return switch (category) {
            case "home" -> homeDeleteResult(homes.deleteHome(viewerId, name), name);
            case "player_warp" -> warpDeleteResult(warps.deleteWarp(viewerId, name), name);
            case "shop" -> shopDeleteResult(shops.deleteShop(viewerId, name), name);
            case "outpost" -> outpostDeleteResult(outposts.deleteOutpost(viewerId, name), name);
            default -> DialogActionRouteResult.unknownAction();
        };
    }

    private DialogActionRouteResult homeDeleteResult(HomeResult result, String name) {
        return result.status() == HomeResult.Status.DELETED
                ? DialogActionRouteResult.message("Deleted home " + name + ".")
                : DialogActionRouteResult.notFound();
    }

    private DialogActionRouteResult warpDeleteResult(PlayerWarpResult result, String name) {
        return result.status() == PlayerWarpResult.Status.DELETED
                ? DialogActionRouteResult.message("Deleted warp " + name + ".")
                : DialogActionRouteResult.notFound();
    }

    private DialogActionRouteResult shopDeleteResult(ShopWarpResult result, String name) {
        return result.status() == ShopWarpResult.Status.DELETED
                ? DialogActionRouteResult.message("Deleted shop " + name + ".")
                : DialogActionRouteResult.notFound();
    }

    private DialogActionRouteResult outpostDeleteResult(OutpostResult result, String name) {
        return result.status() == OutpostResult.Status.DELETED
                ? DialogActionRouteResult.message("Deleted outpost " + name + ".")
                : DialogActionRouteResult.notFound();
    }

    private Optional<TeleportLocation> resolve(UUID viewerId, String category, String name) {
        return switch (category) {
            case "home" -> homes.resolveHome(viewerId, name);
            case "player_warp" -> warps.resolveVisibleWarp(viewerId, name);
            case "shop" -> shops.resolveVisibleShop(viewerId, name);
            case "outpost" -> outposts.resolveOutpost(viewerId, name);
            default -> Optional.empty();
        };
    }
}
