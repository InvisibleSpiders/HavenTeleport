package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.warp.PlayerWarpService;
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
        if (!"teleport".equals(parts[0]) && !"edit".equals(parts[0])) {
            return DialogActionRouteResult.unknownAction();
        }

        Optional<TeleportLocation> location = resolve(viewerId, parts[1], parts[2]);
        if (location.isEmpty()) {
            return DialogActionRouteResult.notFound();
        }

        TeleportLocation resolved = location.orElseThrow();
        return switch (parts[0]) {
            case "teleport" -> DialogActionRouteResult.teleport(resolved);
            case "edit" -> resolved.owner().playerIdOptional().filter(viewerId::equals).isPresent()
                    ? DialogActionRouteResult.showMenu(menus.editMenu(resolved))
                    : DialogActionRouteResult.accessDenied();
            default -> DialogActionRouteResult.unknownAction();
        };
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
