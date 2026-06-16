package com.nick.teleportlocations.tpa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class TeleportWarmupServiceTest {

    private Plugin plugin;
    private BukkitScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        scheduler = mock(BukkitScheduler.class);
        Server server = mock(Server.class);
        plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);

        BukkitTask task = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(task);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong()))
                .thenReturn(task);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(false);
    }

    @Test
    void bypassPermissionSkipsWarmup() {
        when(player.hasPermission("haventeleport.warmup.bypass")).thenReturn(true);
        TeleportWarmupService service = new TeleportWarmupService(plugin, 5, false);
        AtomicBoolean called = new AtomicBoolean(false);

        service.begin(player, () -> called.set(true));

        assert called.get() : "action should have been called immediately for bypass player";
        verify(scheduler, never()).runTaskLater(any(), any(Runnable.class), anyLong());
        verify(scheduler, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void actionBarClearedOnComplete() {
        TeleportWarmupService service = new TeleportWarmupService(plugin, 3, false);

        service.begin(player, () -> {});

        // Capture and invoke the completion task runnable
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), completionCaptor.capture(), eq(3 * 20L));
        completionCaptor.getValue().run();

        verify(player).sendActionBar(Component.empty());
    }

    @Test
    void actionBarClearedOnCancel() {
        TeleportWarmupService service = new TeleportWarmupService(plugin, 3, false);

        service.begin(player, () -> {});
        service.cancel(player.getUniqueId(), false, player);

        verify(player).sendActionBar(Component.empty());
    }
}
