package com.nick.teleportlocations.teleport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.teleport.effect.TeleportEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class ManagedTeleportServiceTest {
    @Test
    void playsDepartureAndArrivalWhenTeleportSucceeds() {
        Player player = mock(Player.class);
        Location destination = mock(Location.class);
        RecordingEffects effects = new RecordingEffects();
        ManagedTeleportService service = new ManagedTeleportService(effects, Runnable::run);
        when(player.teleportAsync(destination)).thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Boolean> result = service.teleport(player, destination);

        assertThat(result.join()).isTrue();
        assertThat(effects.events()).containsExactly("departure", "arrival");
    }

    @Test
    void playsDeniedWhenTeleportReturnsFalse() {
        Player player = mock(Player.class);
        Location destination = mock(Location.class);
        RecordingEffects effects = new RecordingEffects();
        ManagedTeleportService service = new ManagedTeleportService(effects, Runnable::run);
        when(player.teleportAsync(destination)).thenReturn(CompletableFuture.completedFuture(false));

        CompletableFuture<Boolean> result = service.teleport(player, destination);

        assertThat(result.join()).isFalse();
        assertThat(effects.events()).containsExactly("departure", "denied");
    }

    @Test
    void playsDeniedWhenTeleportThrowsImmediately() {
        Player player = mock(Player.class);
        Location destination = mock(Location.class);
        RecordingEffects effects = new RecordingEffects();
        ManagedTeleportService service = new ManagedTeleportService(effects, Runnable::run);
        when(player.teleportAsync(destination)).thenThrow(new IllegalStateException("offline"));

        CompletableFuture<Boolean> result = service.teleport(player, destination);

        assertThat(result.join()).isFalse();
        assertThat(effects.events()).containsExactly("departure", "denied");
    }

    private static final class RecordingEffects implements TeleportEffectService {
        private final List<String> events = new ArrayList<>();

        @Override
        public void departure(Player player) {
            events.add("departure");
        }

        @Override
        public void arrival(Player player) {
            events.add("arrival");
        }

        @Override
        public void denied(Player player) {
            events.add("denied");
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }
}
