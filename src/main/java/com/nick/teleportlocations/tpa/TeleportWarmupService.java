package com.nick.teleportlocations.tpa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private record WarmupState(
            BukkitTask completionTask,
            BukkitTask animationTask,
            long startMs,
            long durationMs,
            AtomicBoolean done) {}

    private final Map<UUID, WarmupState> activeWarmups = new ConcurrentHashMap<>();

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
        cancelSilent(playerId);

        player.sendMessage(Component.text("Teleporting in " + warmupSeconds + "s. Do not move.", NamedTextColor.YELLOW));

        long startMs = System.currentTimeMillis();
        long durationMs = warmupSeconds * 1000L;
        AtomicBoolean done = new AtomicBoolean(false);

        // Placeholder — real state stored after both tasks are created.
        // We use a single-element array to allow capture in lambdas.
        WarmupState[] stateHolder = new WarmupState[1];

        BukkitTask animTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            WarmupState state = stateHolder[0];
            if (state == null || state.done().get()) return;

            long elapsed = System.currentTimeMillis() - startMs;
            double progress = Math.min(1.0, (double) elapsed / durationMs);
            int filled = (int) Math.round(progress * barWidth);
            int empty = barWidth - filled;
            String bar = filledChar.repeat(Math.max(0, filled)) + emptyChar.repeat(Math.max(0, empty));
            long secondsLeft = Math.max(0, (durationMs - elapsed + 999) / 1000);
            Component actionBar = Component.text("Teleporting " + bar + " " + secondsLeft + "s", NamedTextColor.YELLOW);
            player.sendActionBar(actionBar);
        }, 0L, 2L);

        BukkitTask completionTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            WarmupState state = activeWarmups.remove(playerId);
            if (state != null) {
                state.done().set(true);
                state.animationTask().cancel();
            }
            player.sendActionBar(Component.empty());
            action.run();
        }, warmupSeconds * 20L);

        WarmupState state = new WarmupState(completionTask, animTask, startMs, durationMs, done);
        stateHolder[0] = state;
        activeWarmups.put(playerId, state);
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

    /**
     * Silently cancels any active warmup for the given player (used internally when
     * begin() displaces a prior warmup). Clears the action bar via a server lookup so
     * the displaced warmup's progress bar does not remain on screen.
     */
    private void cancelSilent(UUID playerId) {
        boolean had = cancelInternal(playerId);
        if (had) {
            Player onlinePlayer = plugin.getServer().getPlayer(playerId);
            if (onlinePlayer != null) {
                onlinePlayer.sendActionBar(Component.empty());
            }
        }
    }

    /**
     * Cancels warmup and animation tasks.
     * Returns true if a warmup was active.
     */
    private boolean cancelInternal(UUID playerId) {
        WarmupState state = activeWarmups.remove(playerId);
        if (state == null) return false;
        state.done().set(true);
        state.completionTask().cancel();
        state.animationTask().cancel();
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!cancelOnMove || event.getTo() == null || !activeWarmups.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        if (changedBlock(event.getFrom(), event.getTo())) {
            Player player = event.getPlayer();
            boolean had = cancelInternal(player.getUniqueId());
            if (had) {
                player.sendActionBar(Component.empty());
                player.sendMessage(MiniMessage.miniMessage().deserialize(cancelMessage));
            }
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
