package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

final class PlayerLocationCommandTest {
    @Test
    void convertsBukkitLocationToSavedPosition() {
        UUID worldId = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");

        SavedPosition position = PlayerLocationCommand.savedPosition(
                new Location(world, 1.0, 64.0, 2.0, 90.0f, 10.0f)
        );

        assertThat(position.worldId()).isEqualTo(worldId);
        assertThat(position.worldName()).isEqualTo("world");
        assertThat(position.x()).isEqualTo(1.0);
        assertThat(position.y()).isEqualTo(64.0);
        assertThat(position.z()).isEqualTo(2.0);
        assertThat(position.yaw()).isEqualTo(90.0f);
        assertThat(position.pitch()).isEqualTo(10.0f);
    }
}
