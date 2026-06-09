package com.nick.teleportlocations.tpa;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class TeleportWarmupService implements Listener {
    private final Plugin plugin;
    private final int warmupSeconds;
    private final boolean cancelOnMove;
    private final Map<UUID, BukkitTask> warmups = new HashMap<>();

    public TeleportWarmupService(Plugin plugin, int warmupSeconds, boolean cancelOnMove) {
        this.plugin = plugin;
        this.warmupSeconds = Math.max(0, warmupSeconds);
        this.cancelOnMove = cancelOnMove;
    }

    public void begin(Player player, Runnable action) {
        if (warmupSeconds <= 0) {
            action.run();
            return;
        }
        UUID playerId = player.getUniqueId();
        cancel(playerId, false);
        player.sendMessage(Component.text("Teleporting in " + warmupSeconds + "s. Do not move.", NamedTextColor.YELLOW));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            warmups.remove(playerId);
            action.run();
        }, warmupSeconds * 20L);
        warmups.put(playerId, task);
    }

    public void cancel(UUID playerId, boolean notify, Player player) {
        BukkitTask task = warmups.remove(playerId);
        if (task != null) {
            task.cancel();
            if (notify) {
                player.sendMessage(Component.text("Teleport cancelled.", NamedTextColor.RED));
            }
        }
    }

    private void cancel(UUID playerId, boolean notify) {
        BukkitTask task = warmups.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!cancelOnMove || event.getTo() == null || !warmups.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        if (changedBlock(event.getFrom(), event.getTo())) {
            cancel(event.getPlayer().getUniqueId(), true, event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
    }

    private static boolean changedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
