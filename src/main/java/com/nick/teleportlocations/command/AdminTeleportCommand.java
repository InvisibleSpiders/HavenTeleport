package com.nick.teleportlocations.command;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.serverwarp.ServerWarpResult;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.spawn.SpawnResult;
import com.nick.teleportlocations.spawn.SpawnService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminTeleportCommand implements CommandExecutor {
    private final SpawnService spawn;
    private final LimitService limits;
    private final ServerWarpService serverWarps;
    private final PlayerLookup players;

    public AdminTeleportCommand(SpawnService spawn, LimitService limits, ServerWarpService serverWarps, PlayerLookup players) {
        this.spawn = spawn;
        this.limits = limits;
        this.serverWarps = serverWarps;
        this.players = players;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (isLimitCommand(args)) {
            handleLimits(sender, args);
            return true;
        }
        if (isSetSpawn(args)) {
            setSpawn(sender);
            return true;
        }
        if (isServerWarpCommand(args)) {
            handleServerWarp(sender, args);
            return true;
        }
        sender.sendMessage(Component.text(CommandMessages.adminUsage(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean isLimitCommand(String[] args) {
        return args.length >= 5
                && "admin".equals(args[0].toLowerCase(Locale.ROOT))
                && "limits".equals(args[1].toLowerCase(Locale.ROOT));
    }

    private boolean isSetSpawn(String[] args) {
        if (args.length == 1) {
            return "setspawn".equalsIgnoreCase(args[0]);
        }
        return args.length == 2
                && "admin".equals(args[0].toLowerCase(Locale.ROOT))
                && "setspawn".equals(args[1].toLowerCase(Locale.ROOT));
    }

    private boolean isServerWarpCommand(String[] args) {
        return args.length >= 3
                && "admin".equals(args[0].toLowerCase(Locale.ROOT))
                && "serverwarp".equals(args[1].toLowerCase(Locale.ROOT));
    }

    private void setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can set spawn.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("teleportlocations.admin.spawn")) {
            player.sendMessage(Component.text("You do not have permission to set spawn.", NamedTextColor.RED));
            return;
        }
        SpawnResult result = spawn.setSpawn(BukkitLocations.save(player.getLocation()));
        if (result.status() == SpawnResult.Status.UPDATED) {
            player.sendMessage(Component.text("Spawn updated.", NamedTextColor.GREEN));
        }
    }

    private void handleLimits(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleportlocations.admin.limits")) {
            sender.sendMessage(Component.text("You do not have permission to edit teleport limits.", NamedTextColor.RED));
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        Optional<UUID> playerId = players.find(args[3]);
        if (playerId.isEmpty()) {
            sender.sendMessage(Component.text("Player " + args[3] + " was not found.", NamedTextColor.RED));
            return;
        }

        String category = args[4].toLowerCase(Locale.ROOT);
        if (!limits.hasCategory(category)) {
            sender.sendMessage(Component.text("Unknown category. Valid categories: " + String.join(", ", limits.categoryKeys()), NamedTextColor.RED));
            return;
        }

        if ("get".equals(action)) {
            int limit = limits.resolveLimit(playerId.orElseThrow(), category);
            sender.sendMessage(Component.text("Limit for " + args[3] + " in " + category + " is " + limit + ".", NamedTextColor.GREEN));
            return;
        }

        if (args.length < 6) {
            sender.sendMessage(Component.text("Usage: /ht admin limits <set|add|remove|get> <player> <category> [amount]", NamedTextColor.YELLOW));
            return;
        }

        Optional<Integer> amount = parseAmount(args[5]);
        if (amount.isEmpty()) {
            sender.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return;
        }

        switch (action) {
            case "set" -> limits.setLimit(playerId.orElseThrow(), category, amount.orElseThrow());
            case "add" -> limits.addLimit(playerId.orElseThrow(), category, amount.orElseThrow());
            case "remove" -> limits.removeLimit(playerId.orElseThrow(), category, amount.orElseThrow());
            default -> {
                sender.sendMessage(Component.text("Usage: /ht admin limits <set|add|remove|get> <player> <category> [amount]", NamedTextColor.YELLOW));
                return;
            }
        }

        int limit = limits.resolveLimit(playerId.orElseThrow(), category);
        sender.sendMessage(Component.text("Limit for " + args[3] + " in " + category + " is now " + limit + ".", NamedTextColor.GREEN));
    }

    private void handleServerWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teleportlocations.admin.serverwarp")) {
            sender.sendMessage(Component.text("You do not have permission to edit server warps.", NamedTextColor.RED));
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "set" -> setServerWarp(sender, args);
            case "delete" -> deleteServerWarp(sender, args);
            case "list" -> listServerWarps(sender);
            default -> sender.sendMessage(Component.text("Usage: /ht admin serverwarp <set|delete|list> [name]", NamedTextColor.YELLOW));
        }
    }

    private void setServerWarp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /ht admin serverwarp set <name>", NamedTextColor.YELLOW));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can set server warps.", NamedTextColor.RED));
            return;
        }
        ServerWarpResult result = serverWarps.setWarp(args[3], BukkitLocations.save(player.getLocation()));
        switch (result.status()) {
            case CREATED -> player.sendMessage(Component.text("Server warp " + args[3] + " set.", NamedTextColor.GREEN));
            case UPDATED -> player.sendMessage(Component.text("Server warp " + args[3] + " updated.", NamedTextColor.GREEN));
            case DELETED, NOT_FOUND -> player.sendMessage(Component.text("Server warp " + args[3] + " could not be saved.", NamedTextColor.RED));
        }
    }

    private void deleteServerWarp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /ht admin serverwarp delete <name>", NamedTextColor.YELLOW));
            return;
        }
        ServerWarpResult result = serverWarps.deleteWarp(args[3]);
        switch (result.status()) {
            case DELETED -> sender.sendMessage(Component.text("Server warp " + args[3] + " deleted.", NamedTextColor.GREEN));
            case NOT_FOUND -> sender.sendMessage(Component.text("Server warp " + args[3] + " was not found.", NamedTextColor.RED));
            case CREATED, UPDATED -> sender.sendMessage(Component.text("Server warp " + args[3] + " could not be deleted.", NamedTextColor.RED));
        }
    }

    private void listServerWarps(CommandSender sender) {
        String names = serverWarps.visibleWarps().stream()
                .map(location -> location.name())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        sender.sendMessage(Component.text("Server warps: " + names + ".", NamedTextColor.GREEN));
    }

    private Optional<Integer> parseAmount(String input) {
        try {
            return Optional.of(Math.max(0, Integer.parseInt(input)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
