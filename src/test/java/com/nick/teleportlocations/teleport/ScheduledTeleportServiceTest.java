package com.nick.teleportlocations.teleport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nick.teleportlocations.tpa.TeleportWarmupService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class ScheduledTeleportServiceTest {
    @Test
    void beginsWarmupBeforeManagedTeleport() {
        TeleportWarmupService warmups = mock(TeleportWarmupService.class);
        ManagedTeleportService teleports = mock(ManagedTeleportService.class);
        ScheduledTeleportService service = new ScheduledTeleportService(warmups, teleports);
        Player player = mock(Player.class);
        Location destination = mock(Location.class);

        service.teleport(player, destination);

        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(warmups).begin(org.mockito.Mockito.eq(player), action.capture());
        action.getValue().run();
        verify(teleports).teleport(player, destination);
    }
}
