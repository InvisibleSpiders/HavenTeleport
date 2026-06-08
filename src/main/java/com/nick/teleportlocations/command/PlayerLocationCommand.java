package com.nick.teleportlocations.command;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.cost.ChargeResult;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.home.HomeResult;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.outpost.OutpostResult;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.shop.ShopWarpResult;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.teleport.TeleportChargeMessages;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import com.nick.teleportlocations.warp.PlayerWarpResult;
import com.nick.teleportlocations.warp.PlayerWarpService;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerLocationCommand implements CommandExecutor {
    private final HomeService homes;
    private final PlayerWarpService warps;
    private final ShopWarpService shops;
    private final OutpostService outposts;
    private final ServerWarpService serverWarps;
    private final SpawnService spawn;
    private final TeleportChargeService charges;
    private final DialogMenuService dialogs;
    private final PaperDialogPresenter presenter;

    public PlayerLocationCommand(HomeService homes, PlayerWarpService warps, ShopWarpService shops, OutpostService outposts, ServerWarpService serverWarps, SpawnService spawn, TeleportChargeService charges, DialogMenuService dialogs, PaperDialogPresenter presenter) {
        this.homes = homes;
        this.warps = warps;
        this.shops = shops;
        this.outposts = outposts;
        this.serverWarps = serverWarps;
        this.spawn = spawn;
        this.charges = charges;
        this.dialogs = dialogs;
        this.presenter = presenter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        switch (commandName) {
            case "sethome" -> setHome(player, args);
            case "home" -> teleportHome(player, args);
            case "delhome" -> deleteHome(player, args);
            case "mainhome" -> setMainHome(player, args);
            case "homes" -> presenter.show(player, dialogs.homesMenu(player.getUniqueId(), homes.listHomes(player.getUniqueId())));
            case "setwarp" -> setWarp(player, args);
            case "warp" -> teleportWarp(player, args);
            case "delwarp" -> deleteWarp(player, args);
            case "warps" -> presenter.show(player, dialogs.warpsMenu(
                    player.getUniqueId(),
                    serverWarps.visibleWarps(),
                    warps.visibleWarps(player.getUniqueId())
            ));
            case "setshop" -> setShop(player, args);
            case "delshop" -> deleteShop(player, args);
            case "shops" -> presenter.show(player, dialogs.shopWarpsMenu(player.getUniqueId(), shops.visibleShops(player.getUniqueId())));
            case "setoutpost" -> setOutpost(player, args);
            case "outpost" -> teleportOutpost(player, args);
            case "deloutpost" -> deleteOutpost(player, args);
            case "spawn" -> teleportSpawn(player);
            default -> player.sendMessage(Component.text(CommandMessages.playerUsage(), NamedTextColor.YELLOW));
        }
        return true;
    }

    private void setHome(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /sethome <name>", NamedTextColor.YELLOW));
            return;
        }
        HomeResult result = homes.setHome(
                player.getUniqueId(),
                args[0],
                BukkitLocations.save(player.getLocation()),
                player.hasPermission("teleportlocations.admin.bypass.creation")
        );
        sendHomeResult(player, result, args[0]);
    }

    private void teleportHome(Player player, String[] args) {
        String name = args.length == 0 ? "" : args[0];
        Optional<TeleportLocation> home = homes.resolveHome(player.getUniqueId(), name);
        if (home.isEmpty()) {
            player.sendMessage(Component.text("Home not found.", NamedTextColor.RED));
            return;
        }
        Location destination = BukkitLocations.load(home.orElseThrow().position());
        if (destination == null) {
            player.sendMessage(Component.text("That home world is not loaded.", NamedTextColor.RED));
            return;
        }
        player.teleportAsync(destination);
        player.sendMessage(Component.text("Teleported to home " + home.orElseThrow().name() + ".", NamedTextColor.GREEN));
    }

    private void deleteHome(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /delhome <name>", NamedTextColor.YELLOW));
            return;
        }
        sendHomeResult(player, homes.deleteHome(player.getUniqueId(), args[0]), args[0]);
    }

    private void setMainHome(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /mainhome <name>", NamedTextColor.YELLOW));
            return;
        }
        sendHomeResult(player, homes.setMainHome(player.getUniqueId(), args[0]), args[0]);
    }

    private void setWarp(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /setwarp <name>", NamedTextColor.YELLOW));
            return;
        }
        PlayerWarpResult result = warps.setWarp(
                player.getUniqueId(),
                args[0],
                BukkitLocations.save(player.getLocation()),
                player.hasPermission("teleportlocations.admin.bypass.creation")
        );
        sendWarpResult(player, result, args[0]);
    }

    private void teleportWarp(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /warp <name>", NamedTextColor.YELLOW));
            return;
        }
        Optional<TeleportLocation> warp = serverWarps.resolveVisibleWarp(args[0])
                .or(() -> warps.resolveVisibleWarp(player.getUniqueId(), args[0]));
        if (warp.isEmpty()) {
            player.sendMessage(Component.text("Warp not found.", NamedTextColor.RED));
            return;
        }
        Location destination = BukkitLocations.load(warp.orElseThrow().position());
        if (destination == null) {
            player.sendMessage(Component.text("That warp world is not loaded.", NamedTextColor.RED));
            return;
        }
        if (!charge(player, warp.orElseThrow())) {
            return;
        }
        player.teleportAsync(destination);
        player.sendMessage(Component.text("Teleported to warp " + warp.orElseThrow().name() + ".", NamedTextColor.GREEN));
    }

    private boolean charge(Player player, TeleportLocation location) {
        ChargeResult charge = charges.chargeIfNeeded(
                player.getUniqueId(),
                player.hasPermission("teleportlocations.admin.bypass.cost"),
                location
        );
        if (charge.success()) {
            return true;
        }
        player.sendMessage(Component.text(TeleportChargeMessages.failure(charge.reason()), NamedTextColor.RED));
        return false;
    }

    private void deleteWarp(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /delwarp <name>", NamedTextColor.YELLOW));
            return;
        }
        sendWarpResult(player, warps.deleteWarp(player.getUniqueId(), args[0]), args[0]);
    }

    private void setShop(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /setshop <name>", NamedTextColor.YELLOW));
            return;
        }
        ShopWarpResult result = shops.setShop(
                player.getUniqueId(),
                args[0],
                BukkitLocations.save(player.getLocation()),
                player.hasPermission("teleportlocations.admin.bypass.creation")
        );
        sendShopResult(player, result, args[0]);
    }

    private void deleteShop(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /delshop <name>", NamedTextColor.YELLOW));
            return;
        }
        sendShopResult(player, shops.deleteShop(player.getUniqueId(), args[0]), args[0]);
    }

    private void setOutpost(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /setoutpost <name>", NamedTextColor.YELLOW));
            return;
        }
        OutpostResult result = outposts.setOutpost(
                player.getUniqueId(),
                args[0],
                BukkitLocations.save(player.getLocation()),
                player.hasPermission("teleportlocations.admin.bypass.creation")
        );
        sendOutpostResult(player, result, args[0]);
    }

    private void teleportOutpost(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /outpost <name>", NamedTextColor.YELLOW));
            return;
        }
        Optional<TeleportLocation> outpost = outposts.resolveOutpost(player.getUniqueId(), args[0]);
        if (outpost.isEmpty()) {
            player.sendMessage(Component.text("Outpost not found.", NamedTextColor.RED));
            return;
        }
        Location destination = BukkitLocations.load(outpost.orElseThrow().position());
        if (destination == null) {
            player.sendMessage(Component.text("That outpost world is not loaded.", NamedTextColor.RED));
            return;
        }
        player.teleportAsync(destination);
        player.sendMessage(Component.text("Teleported to outpost " + outpost.orElseThrow().name() + ".", NamedTextColor.GREEN));
    }

    private void deleteOutpost(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /deloutpost <name>", NamedTextColor.YELLOW));
            return;
        }
        sendOutpostResult(player, outposts.deleteOutpost(player.getUniqueId(), args[0]), args[0]);
    }

    private void teleportSpawn(Player player) {
        Optional<TeleportLocation> configuredSpawn = spawn.spawn();
        if (configuredSpawn.isEmpty()) {
            player.sendMessage(Component.text("Spawn has not been set.", NamedTextColor.RED));
            return;
        }
        Location destination = BukkitLocations.load(configuredSpawn.orElseThrow().position());
        if (destination == null) {
            player.sendMessage(Component.text("The spawn world is not loaded.", NamedTextColor.RED));
            return;
        }
        player.teleportAsync(destination);
        player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));
    }

    private void sendHomeResult(Player player, HomeResult result, String name) {
        switch (result.status()) {
            case CREATED -> player.sendMessage(Component.text("Home " + name + " set.", NamedTextColor.GREEN));
            case UPDATED -> player.sendMessage(Component.text("Home " + name + " updated.", NamedTextColor.GREEN));
            case DELETED -> player.sendMessage(Component.text("Home " + name + " deleted.", NamedTextColor.GREEN));
            case NOT_FOUND -> player.sendMessage(Component.text("Home " + name + " was not found.", NamedTextColor.RED));
            case LIMIT_REACHED -> player.sendMessage(Component.text("You have reached your home limit.", NamedTextColor.RED));
            case CLAIM_DENIED -> player.sendMessage(Component.text("You cannot create a home here.", NamedTextColor.RED));
        }
    }

    private void sendWarpResult(Player player, PlayerWarpResult result, String name) {
        switch (result.status()) {
            case CREATED -> player.sendMessage(Component.text("Warp " + name + " set.", NamedTextColor.GREEN));
            case UPDATED -> player.sendMessage(Component.text("Warp " + name + " updated.", NamedTextColor.GREEN));
            case DELETED -> player.sendMessage(Component.text("Warp " + name + " deleted.", NamedTextColor.GREEN));
            case NOT_FOUND -> player.sendMessage(Component.text("Warp " + name + " was not found.", NamedTextColor.RED));
            case LIMIT_REACHED -> player.sendMessage(Component.text("You have reached your player warp limit.", NamedTextColor.RED));
            case CLAIM_DENIED -> player.sendMessage(Component.text("You cannot create a warp here.", NamedTextColor.RED));
        }
    }

    private void sendShopResult(Player player, ShopWarpResult result, String name) {
        switch (result.status()) {
            case CREATED -> player.sendMessage(Component.text("Shop " + name + " set.", NamedTextColor.GREEN));
            case UPDATED -> player.sendMessage(Component.text("Shop " + name + " updated.", NamedTextColor.GREEN));
            case DELETED -> player.sendMessage(Component.text("Shop " + name + " deleted.", NamedTextColor.GREEN));
            case NOT_FOUND -> player.sendMessage(Component.text("Shop " + name + " was not found.", NamedTextColor.RED));
            case LIMIT_REACHED -> player.sendMessage(Component.text("You have reached your shop warp limit.", NamedTextColor.RED));
            case CLAIM_DENIED -> player.sendMessage(Component.text("You cannot create a shop warp here.", NamedTextColor.RED));
        }
    }

    private void sendOutpostResult(Player player, OutpostResult result, String name) {
        switch (result.status()) {
            case CREATED -> player.sendMessage(Component.text("Outpost " + name + " set.", NamedTextColor.GREEN));
            case UPDATED -> player.sendMessage(Component.text("Outpost " + name + " updated.", NamedTextColor.GREEN));
            case DELETED -> player.sendMessage(Component.text("Outpost " + name + " deleted.", NamedTextColor.GREEN));
            case NOT_FOUND -> player.sendMessage(Component.text("Outpost " + name + " was not found.", NamedTextColor.RED));
            case LIMIT_REACHED -> player.sendMessage(Component.text("You have reached your outpost limit.", NamedTextColor.RED));
            case CLAIM_DENIED -> player.sendMessage(Component.text("You cannot create an outpost here.", NamedTextColor.RED));
        }
    }

}
