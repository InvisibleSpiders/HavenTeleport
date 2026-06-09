package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.tpa.TeleportRequestService;
import com.nick.teleportlocations.tpa.TeleportWarmupService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class TeleportRequestCommandTest {
    @Test
    void requestSendsClickableAcceptAndDeclineActions() {
        Player requester = player("Nova", UUID.randomUUID(), location("world"));
        Player target = player("Ari", UUID.randomUUID(), location("world"));
        TeleportRequestCommand command = command(Map.of("Nova", requester, "Ari", target));

        command.onCommand(requester, command("tpa"), "tpa", new String[] {"Ari"});

        ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(target).sendMessage(message.capture());
        assertThat(PlainTextComponentSerializer.plainText().serialize(message.getValue()))
                .contains("Nova wants to teleport to you.", "[Accept]", "[Decline]");
        assertThat(message.getValue().children())
                .anySatisfy(child -> assertThat(child.clickEvent()).isNotNull()
                        .isEqualTo(ClickEvent.runCommand("/tpaccept Nova")))
                .anySatisfy(child -> assertThat(child.clickEvent()).isNotNull()
                        .isEqualTo(ClickEvent.runCommand("/tpdecline Nova")));
    }

    @Test
    void acceptingTpaHereMovesReceiverToRequester() {
        Player requester = player("Nova", UUID.randomUUID(), location("world"));
        Player receiver = player("Ari", UUID.randomUUID(), location("world"));
        TeleportRequestCommand command = command(Map.of("Nova", requester, "Ari", receiver));
        command.onCommand(requester, command("tpahere"), "tpahere", new String[] {"Ari"});

        command.onCommand(receiver, command("tpaccept"), "tpaccept", new String[] {"Nova"});

        verify(receiver).teleportAsync(requester.getLocation());
    }

    @Test
    void destinationQuitCancelsWarmupAndMessagesMovingPlayer() {
        Player requester = player("Nova", UUID.randomUUID(), location("world"));
        Player receiver = player("Ari", UUID.randomUUID(), location("world"));
        TeleportWarmupService warmups = mock(TeleportWarmupService.class);
        TeleportRequestCommand command = command(Map.of("Nova", requester, "Ari", receiver), warmups);
        command.onCommand(requester, command("tpa"), "tpa", new String[] {"Ari"});
        command.onCommand(receiver, command("tpaccept"), "tpaccept", new String[] {"Nova"});

        command.onQuit(quit(receiver));

        verify(warmups).cancel(
                requester.getUniqueId(),
                "Teleport cancelled: Ari logged off.",
                requester
        );
        verify(requester, never()).teleportAsync(receiver.getLocation());
    }

    private static TeleportRequestCommand command(Map<String, Player> players) {
        return command(players, new TeleportWarmupService(mock(Plugin.class), 0, true));
    }

    private static TeleportRequestCommand command(Map<String, Player> players, TeleportWarmupService warmups) {
        return new TeleportRequestCommand(
                new MapOnlinePlayerLookup(players),
                new TeleportRequestService(60, 0, () -> Instant.EPOCH),
                warmups,
                true
        );
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private static Player player(String name, UUID id, Location location) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getLocation()).thenReturn(location);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.teleportAsync(location)).thenReturn(CompletableFuture.completedFuture(true));
        return player;
    }

    private static org.bukkit.event.player.PlayerQuitEvent quit(Player player) {
        org.bukkit.event.player.PlayerQuitEvent event = mock(org.bukkit.event.player.PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    private static Location location(String worldName) {
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn(worldName);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(1.0);
        when(location.getY()).thenReturn(64.0);
        when(location.getZ()).thenReturn(2.0);
        return location;
    }

    private record MapOnlinePlayerLookup(Map<String, Player> players) implements OnlinePlayerLookup {
        @Override
        public Optional<Player> find(String input) {
            return players.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(input))
                    .map(Map.Entry::getValue)
                    .findFirst();
        }

        @Override
        public Optional<Player> find(UUID playerId) {
            return players.values().stream()
                    .filter(player -> player.getUniqueId().equals(playerId))
                    .findFirst();
        }
    }
}
