package com.nick.teleportlocations.command;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.spawn.SpawnResult;
import com.nick.teleportlocations.spawn.SpawnService;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminTeleportCommand implements CommandExecutor {
    private final SpawnService spawn;

    public AdminTeleportCommand(SpawnService spawn) {
        this.spawn = spawn;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (isSetSpawn(args)) {
            setSpawn(sender);
            return true;
        }
        sender.sendMessage(Component.text(CommandMessages.adminUsage(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean isSetSpawn(String[] args) {
        if (args.length == 1) {
            return "setspawn".equalsIgnoreCase(args[0]);
        }
        return args.length == 2
                && "admin".equals(args[0].toLowerCase(Locale.ROOT))
                && "setspawn".equals(args[1].toLowerCase(Locale.ROOT));
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
}
