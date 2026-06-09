package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class AdminTeleportCommandTest {
    @Test
    void adminCanSetAddRemoveAndGetPlayerCategoryLimit() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        CommandSender sender = adminSender();

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "set", "Nova", "home", "7"});
        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "add", "Nova", "home", "2"});
        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "remove", "Nova", "home", "3"});
        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "get", "Nova", "home"});

        assertThat(fixture.limits.resolveLimit(playerId, "home")).isEqualTo(6);
    }

    @Test
    void rejectsUnknownLimitCategoryWithoutChangingDefaults() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        CommandSender sender = adminSender();

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "set", "Nova", "missing", "7"});

        assertThat(fixture.limits.resolveLimit(playerId, "home")).isEqualTo(3);
    }

    @Test
    void deniesLimitChangesWithoutAdminPermission() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("teleportlocations.admin.limits")).thenReturn(false);

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "limits", "set", "Nova", "home", "7"});

        assertThat(fixture.limits.resolveLimit(playerId, "home")).isEqualTo(3);
    }

    @Test
    void adminCanSetAndDeleteServerWarp() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        Player sender = adminPlayerAt(location("world"));

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "serverwarp", "set", "market"});
        assertThat(fixture.serverWarps.visibleWarps()).extracting("name").containsExactly("market");

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "serverwarp", "delete", "market"});
        assertThat(fixture.serverWarps.visibleWarps()).isEmpty();
    }

    @Test
    void adminCanToggleClaimBypassMode() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        Player sender = adminBypassPlayer(playerId);

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "bypass", "claims", "on"});
        assertThat(fixture.bypass.claims(playerId)).isTrue();

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "bypass", "claims", "off"});
        assertThat(fixture.bypass.claims(playerId)).isFalse();

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "bypass", "claims"});
        assertThat(fixture.bypass.claims(playerId)).isTrue();
    }

    @Test
    void adminCanTeleportPlayerToTargetWithoutRequestWarmupOrCooldown() {
        UUID playerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Player player = onlinePlayer(playerId, location("world"));
        Player target = onlinePlayer(targetId, location("world"));
        Fixture fixture = Fixture.create(Map.of("Nova", player, "Ari", target));
        CommandSender sender = adminTeleportSender();

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "tp", "Nova", "Ari"});

        verify(player).teleportAsync(target.getLocation());
    }

    @Test
    void deniesClaimBypassWithoutPermission() {
        UUID playerId = UUID.randomUUID();
        Fixture fixture = Fixture.create("Nova", playerId);
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(playerId);
        when(sender.hasPermission("teleportlocations.admin.bypass.claims")).thenReturn(false);

        fixture.command.onCommand(sender, command("ht"), "ht", new String[] {"admin", "bypass", "claims", "on"});

        assertThat(fixture.bypass.claims(playerId)).isFalse();
    }

    private static CommandSender adminSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("teleportlocations.admin.limits")).thenReturn(true);
        return sender;
    }

    private static CommandSender adminTeleportSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("teleportlocations.admin.teleport")).thenReturn(true);
        return sender;
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private static Player adminPlayerAt(Location location) {
        Player player = mock(Player.class);
        when(player.hasPermission("teleportlocations.admin.serverwarp")).thenReturn(true);
        when(player.getLocation()).thenReturn(location);
        return player;
    }

    private static Player adminBypassPlayer(UUID playerId) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.hasPermission("teleportlocations.admin.bypass.claims")).thenReturn(true);
        return player;
    }

    private static Player onlinePlayer(UUID playerId, Location location) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(location);
        when(player.teleportAsync(location)).thenReturn(CompletableFuture.completedFuture(true));
        return player;
    }

    private static Location location(String worldName) {
        Location location = mock(Location.class);
        World world = world(worldName);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(1.0);
        when(location.getY()).thenReturn(64.0);
        when(location.getZ()).thenReturn(2.0);
        when(location.getYaw()).thenReturn(90.0f);
        when(location.getPitch()).thenReturn(10.0f);
        return location;
    }

    private static World world(String name) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn(name);
        return world;
    }

    private record Fixture(AdminTeleportCommand command, LimitService limits, ServerWarpService serverWarps, AdminBypassService bypass) {
        private static Fixture create(String playerName, UUID playerId) {
            return create(Map.of(playerName, playerWithId(playerId)));
        }

        private static Fixture create(Map<String, Player> onlinePlayers) {
            LimitService limitService = new LimitService(categories(), new InMemoryLimitRepository());
            LocationService locationService = new LocationService(new InMemoryLocationRepository(), () -> Instant.EPOCH);
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    categories(),
                    LandClaimsGateway.fixed(true, true),
                    MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            HomeService homeService = new HomeService(locationService, limitService, creationPolicy);
            SpawnService spawnService = new SpawnService(locationService, homeService);
            ServerWarpService serverWarpService = new ServerWarpService(locationService);
            AdminBypassService bypassService = new AdminBypassService();
            PlayerLookup lookup = name -> onlinePlayers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().getUniqueId())
                    .findFirst();
            OnlinePlayerLookup onlineLookup = new MapOnlinePlayerLookup(onlinePlayers);
            return new Fixture(
                    new AdminTeleportCommand(spawnService, limitService, serverWarpService, bypassService, lookup, onlineLookup),
                    limitService,
                    serverWarpService,
                    bypassService
            );
        }
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

    private static Player playerWithId(UUID playerId) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        return player;
    }

    private static Map<String, CategoryConfig> categories() {
        return Map.of(
                "home", new CategoryConfig("home", OwnerKind.PLAYER, 3, CreationZone.TRUSTED_CLAIM, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false),
                "shop", new CategoryConfig("shop", OwnerKind.PLAYER, 1, CreationZone.TRUSTED_CLAIM, AccessMode.PUBLIC, VisibilityMode.LISTED, false, true, true)
        );
    }
}
