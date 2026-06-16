package com.nick.teleportlocations.tpa;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final String filledChar;
    private final String emptyChar;
    private final int barWidth;
    private final String cancelMessage;

    private final Map<UUID, BukkitTask> warmups = new HashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private final Map<UUID, Long> startTimes = new HashMap<>();

    public TeleportWarmupService(Plugin plugin, int warmupSeconds, boolean cancelOnMove) {
        this(plugin, warmupSeconds, cancelOnMove, "█", "░", 10, "<red>Teleport cancelled.");
    }

    public TeleportWarmupService(Plugin plugin, int warmupSeconds, boolean cancelOnMove,
                                  String filledChar, String emptyChar, int barWidth,
                                  String cancelMessage) {
        this.plugin = plugin;
        this.warmupSeconds = Math.max(0, warmupSeconds);
        this.cancelOnMove = cancelOnMove;
        this.filledChar = filledChar;
        this.emptyChar = emptyChar;
        this.barWidth = barWidth;
        this.cancelMessage = cancelMessage;
    }

    public void begin(Player player, Runnable action) {
        if (player.hasPermission("haventeleport.warmup.bypass")) {
            action.run();
            return;
        }
        if (warmupSeconds <= 0) {
            action.run();
            return;
        }
        UUID playerId = player.getUniqueId();
        cancel(playerId, false);

        player.sendMessage(Component.text("Teleporting in " + warmupSeconds + "s. Do not move.", NamedTextColor.YELLOW));

        long startMs = System.currentTimeMillis();
        long durationMs = warmupSeconds * 1000L;
        startTimes.put(playerId, startMs);

        BukkitTask animTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startMs;
            double progress = Math.min(1.0, (double) elapsed / durationMs);
            int filled = (int) Math.round(progress * barWidth);
            int empty = barWidth - filled;
            String bar = filledChar.repeat(Math.max(0, filled)) + emptyChar.repeat(Math.max(0, empty));
            long secondsLeft = Math.max(0, (durationMs - elapsed + 999) / 1000);
            Component actionBar = Component.text("Teleporting " + bar + " " + secondsLeft + "s", NamedTextColor.YELLOW);
            player.sendActionBar(actionBar);
        }, 0L, 2L);
        animationTasks.put(playerId, animTask);

        BukkitTask completionTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            warmups.remove(playerId);
            startTimes.remove(playerId);
            BukkitTask anim = animationTasks.remove(playerId);
            if (anim != null) {
                anim.cancel();
            }
            player.sendActionBar(Component.empty());
            action.run();
        }, warmupSeconds * 20L);
        warmups.put(playerId, completionTask);
    }

    public void cancel(UUID playerId, boolean notify, Player player) {
        boolean had = cancelInternal(playerId);
        if (had) {
            player.sendActionBar(Component.empty());
            if (notify) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(cancelMessage));
            }
        }
    }

    public void cancel(UUID playerId, String message, Player player) {
        boolean had = cancelInternal(playerId);
        if (had) {
            player.sendActionBar(Component.empty());
            player.sendMessage(Component.text(message, NamedTextColor.RED));
        }
    }

    private void cancel(UUID playerId, boolean notify) {
        cancelInternal(playerId);
    }

    /**
     * Cancels warmup and animation tasks, clears action bar state.
     * Returns true if a warmup was active.
     */
    private boolean cancelInternal(UUID playerId) {
        BukkitTask task = warmups.remove(playerId);
        BukkitTask anim = animationTasks.remove(playerId);
        startTimes.remove(playerId);
        boolean active = task != null;
        if (task != null) {
            task.cancel();
        }
        if (anim != null) {
            anim.cancel();
        }
        return active;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!cancelOnMove || event.getTo() == null || !warmups.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        if (changedBlock(event.getFrom(), event.getTo())) {
            Player player = event.getPlayer();
            cancelInternal(player.getUniqueId());
            player.sendActionBar(Component.empty());
            player.sendMessage(MiniMessage.miniMessage().deserialize(cancelMessage));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelInternal(event.getPlayer().getUniqueId());
    }

    private static boolean changedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
