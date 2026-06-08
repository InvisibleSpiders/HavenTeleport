package com.nick.teleportlocations.command;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.home.HomeResult;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public final class PlayerLocationCommand implements CommandExecutor {
    private final HomeService homes;
    private final SpawnService spawn;
    private final DialogMenuService dialogs;
    private final PaperDialogPresenter presenter;

    public PlayerLocationCommand(HomeService homes, SpawnService spawn, DialogMenuService dialogs, PaperDialogPresenter presenter) {
        this.homes = homes;
        this.spawn = spawn;
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

}
