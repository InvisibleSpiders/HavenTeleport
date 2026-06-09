package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.elevator.ElevatorResult;
import com.nick.teleportlocations.elevator.ElevatorService;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.home.HomeResult;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.outpost.OutpostResult;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.shop.ShopWarpResult;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.teleportblock.TeleportBlockResult;
import com.nick.teleportlocations.teleportblock.TeleportBlockService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import com.nick.teleportlocations.warp.PlayerWarpResult;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

public final class DialogActionRouter {
    private final HomeService homes;
    private final PlayerWarpService warps;
    private final ShopWarpService shops;
    private final OutpostService outposts;
    private final ServerWarpService serverWarps;
    private final ElevatorService elevators;
    private final TeleportBlockService teleportBlocks;
    private final LocationService locations;
    private final DialogMenuService menus;
    private final AdminBypassService bypass;
    private final BiPredicate<UUID, String> permissions;

    public DialogActionRouter(
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            ServerWarpService serverWarps,
            DialogMenuService menus
    ) {
        this(homes, warps, shops, outposts, serverWarps, null, null, null, menus, new AdminBypassService(), (viewerId, permission) -> false);
    }

    public DialogActionRouter(
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            ServerWarpService serverWarps,
            ElevatorService elevators,
            DialogMenuService menus,
            AdminBypassService bypass,
            BiPredicate<UUID, String> permissions
    ) {
        this(homes, warps, shops, outposts, serverWarps, elevators, null, null, menus, bypass, permissions);
    }

    public DialogActionRouter(
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            ServerWarpService serverWarps,
            ElevatorService elevators,
            DialogMenuService menus,
            BiPredicate<UUID, String> permissions
    ) {
        this(homes, warps, shops, outposts, serverWarps, elevators, null, null, menus, new AdminBypassService(), permissions);
    }

    public DialogActionRouter(
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            OutpostService outposts,
            ServerWarpService serverWarps,
            ElevatorService elevators,
            TeleportBlockService teleportBlocks,
            LocationService locations,
            DialogMenuService menus,
            AdminBypassService bypass,
            BiPredicate<UUID, String> permissions
    ) {
        this.homes = homes;
        this.warps = warps;
        this.shops = shops;
        this.outposts = outposts;
        this.serverWarps = serverWarps;
        this.elevators = elevators;
        this.teleportBlocks = teleportBlocks;
        this.locations = locations;
        this.menus = menus;
        this.bypass = Objects.requireNonNull(bypass, "bypass");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
    }

    public DialogActionRouteResult route(UUID viewerId, String actionKey) {
        return route(viewerId, actionKey, DialogInputValues.empty());
    }

    public DialogActionRouteResult route(UUID viewerId, String actionKey, DialogInputValues inputValues) {
        String[] parts = actionKey.split(":");
        if (parts.length < 3) {
            return DialogActionRouteResult.unknownAction();
        }
        return switch (parts[0]) {
            case "teleport", "edit" -> routeLocationAction(viewerId, parts[0], parts[1], parts[2]);
            case "set-main" -> setMainHome(viewerId, parts[1], parts[2]);
            case "delete" -> deleteLocation(viewerId, parts[1], parts[2]);
            case "set-access" -> parts.length == 4
                    ? setPlayerWarpAccess(viewerId, parts[1], parts[2], parts[3])
                    : DialogActionRouteResult.unknownAction();
            case "set-visibility" -> parts.length == 4
                    ? setPlayerWarpVisibility(viewerId, parts[1], parts[2], parts[3])
                    : DialogActionRouteResult.unknownAction();
            case "set-cost" -> parts.length == 5
                    ? setPlayerWarpCost(viewerId, parts[1], parts[2], parts[3], parts[4])
                    : DialogActionRouteResult.unknownAction();
            case "show-cost-editor" -> parts.length == 4
                    ? showPlayerWarpCostEditor(viewerId, parts[1], parts[2], parts[3])
                    : DialogActionRouteResult.unknownAction();
            case "set-cost-input" -> parts.length == 4
                    ? setPlayerWarpCostFromInput(viewerId, parts[1], parts[2], parts[3], inputValues)
                    : DialogActionRouteResult.unknownAction();
            case "set-elevator-particle" -> parts.length == 3
                    ? setElevatorParticle(viewerId, parts[1], parts[2])
                    : DialogActionRouteResult.unknownAction();
            case "set-teleport-block-target" -> parts.length == 3
                    ? setTeleportBlockTarget(viewerId, parts[1], parts[2])
                    : DialogActionRouteResult.unknownAction();
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

    private DialogActionRouteResult setPlayerWarpAccess(UUID viewerId, String category, String name, String access) {
        if (!"player_warp".equals(category)) {
            return DialogActionRouteResult.unknownAction();
        }
        try {
            PlayerWarpResult result = warps.setAccess(viewerId, name, AccessMode.parse(access));
            return result.status() == PlayerWarpResult.Status.UPDATED
                    ? DialogActionRouteResult.message("Warp " + name + " access updated.")
                    : DialogActionRouteResult.notFound();
        } catch (IllegalArgumentException exception) {
            return DialogActionRouteResult.unknownAction();
        }
    }

    private DialogActionRouteResult setPlayerWarpVisibility(UUID viewerId, String category, String name, String visibility) {
        if (!"player_warp".equals(category)) {
            return DialogActionRouteResult.unknownAction();
        }
        try {
            PlayerWarpResult result = warps.setVisibility(viewerId, name, VisibilityMode.parse(visibility));
            return result.status() == PlayerWarpResult.Status.UPDATED
                    ? DialogActionRouteResult.message("Warp " + name + " visibility updated.")
                    : DialogActionRouteResult.notFound();
        } catch (IllegalArgumentException exception) {
            return DialogActionRouteResult.unknownAction();
        }
    }

    private DialogActionRouteResult setPlayerWarpCost(UUID viewerId, String category, String name, String costType, String amount) {
        if (!"player_warp".equals(category)) {
            return DialogActionRouteResult.unknownAction();
        }
        try {
            PlayerWarpResult result = warps.setCost(viewerId, name, parseCost(costType, amount));
            return result.status() == PlayerWarpResult.Status.UPDATED
                    ? DialogActionRouteResult.message("Warp " + name + " cost updated.")
                    : DialogActionRouteResult.notFound();
        } catch (IllegalArgumentException exception) {
            return DialogActionRouteResult.unknownAction();
        }
    }

    private DialogActionRouteResult showPlayerWarpCostEditor(UUID viewerId, String category, String name, String costType) {
        if (!"player_warp".equals(category) || !supportsCustomCost(costType)) {
            return DialogActionRouteResult.unknownAction();
        }
        Optional<TeleportLocation> location = warps.resolveVisibleWarp(viewerId, name);
        if (location.isEmpty()) {
            return DialogActionRouteResult.notFound();
        }
        TeleportLocation resolved = location.orElseThrow();
        if (resolved.owner().playerIdOptional().filter(viewerId::equals).isEmpty()) {
            return DialogActionRouteResult.accessDenied();
        }
        return DialogActionRouteResult.showMenu(menus.customCostMenu(resolved, costType));
    }

    private DialogActionRouteResult setPlayerWarpCostFromInput(UUID viewerId, String category, String name, String costType, DialogInputValues inputValues) {
        Float amount = inputValues.getFloat("amount");
        if (amount == null) {
            return DialogActionRouteResult.unknownAction();
        }
        return setPlayerWarpCost(viewerId, category, name, costType, amount.toString());
    }

    private boolean supportsCustomCost(String costType) {
        return "money".equals(costType) || "xp-levels".equals(costType) || "xp-points".equals(costType);
    }

    private DialogActionRouteResult setElevatorParticle(UUID viewerId, String blockIdValue, String particleValue) {
        if (elevators == null) {
            return DialogActionRouteResult.unknownAction();
        }
        try {
            UUID blockId = UUID.fromString(blockIdValue);
            ElevatorParticle particle = ElevatorParticle.parse(particleValue);
            if (!permissions.test(viewerId, particlePermission(particle))) {
                return DialogActionRouteResult.accessDenied();
            }
            boolean adminBypass = permissions.test(viewerId, "teleportlocations.admin.elevator") && bypass.claims(viewerId);
            ElevatorResult result = elevators.setParticle(viewerId, blockId, particle, adminBypass);
            return switch (result.status()) {
                case UPDATED -> DialogActionRouteResult.message("Elevator particle updated.");
                case NOT_FOUND -> DialogActionRouteResult.notFound();
                case ACCESS_DENIED -> DialogActionRouteResult.accessDenied();
                default -> DialogActionRouteResult.unknownAction();
            };
        } catch (IllegalArgumentException exception) {
            return DialogActionRouteResult.unknownAction();
        }
    }

    public static String particlePermission(ElevatorParticle particle) {
        return "teleportlocations.elevator.particle." + particle.name().toLowerCase(Locale.ROOT);
    }

    private DialogActionRouteResult setTeleportBlockTarget(UUID viewerId, String blockIdValue, String locationIdValue) {
        if (teleportBlocks == null || locations == null) {
            return DialogActionRouteResult.unknownAction();
        }
        if (!permissions.test(viewerId, "teleportlocations.teleportblock.link")) {
            return DialogActionRouteResult.accessDenied();
        }
        try {
            UUID blockId = UUID.fromString(blockIdValue);
            UUID locationId = UUID.fromString(locationIdValue);
            Optional<TeleportLocation> target = locations.findById(locationId);
            if (target.isEmpty()) {
                return DialogActionRouteResult.notFound();
            }
            boolean adminBypass = permissions.test(viewerId, "teleportlocations.admin.teleportblock") && bypass.claims(viewerId);
            TeleportBlockResult result = teleportBlocks.setTargetLocation(viewerId, blockId, target.orElseThrow(), adminBypass);
            return switch (result.status()) {
                case UPDATED -> DialogActionRouteResult.message("Teleport block target updated.");
                case NOT_FOUND -> DialogActionRouteResult.notFound();
                case ACCESS_DENIED -> DialogActionRouteResult.accessDenied();
                default -> DialogActionRouteResult.unknownAction();
            };
        } catch (IllegalArgumentException exception) {
            return DialogActionRouteResult.unknownAction();
        }
    }

    private CostSpec parseCost(String type, String amount) {
        return switch (type) {
            case "free" -> CostSpec.free();
            case "money" -> CostSpec.money(positiveDouble(amount));
            case "xp-levels" -> CostSpec.xpLevels(positiveInt(amount));
            case "xp-points" -> CostSpec.xpPoints(positiveInt(amount));
            default -> throw new IllegalArgumentException("Unsupported cost type");
        };
    }

    private double positiveDouble(String value) {
        double amount = Double.parseDouble(value);
        if (amount < 0.0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        return amount;
    }

    private int positiveInt(String value) {
        int amount = Math.round(Float.parseFloat(value));
        if (amount < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        return amount;
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
            case "server_warp" -> serverWarps.resolveVisibleWarp(name);
            case "shop" -> shops.resolveVisibleShop(viewerId, name);
            case "outpost" -> outposts.resolveOutpost(viewerId, name);
            default -> Optional.empty();
        };
    }
}
