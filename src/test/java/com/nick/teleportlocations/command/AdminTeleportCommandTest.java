package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

    private static CommandSender adminSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("teleportlocations.admin.limits")).thenReturn(true);
        return sender;
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private record Fixture(AdminTeleportCommand command, LimitService limits) {
        private static Fixture create(String playerName, UUID playerId) {
            LimitService limitService = new LimitService(categories(), new InMemoryLimitRepository());
            LocationService locationService = new LocationService(new InMemoryLocationRepository(), () -> Instant.EPOCH);
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    categories(),
                    LandClaimsGateway.fixed(true, true),
                    MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            HomeService homeService = new HomeService(locationService, limitService, creationPolicy);
            SpawnService spawnService = new SpawnService(locationService, homeService);
            PlayerLookup lookup = name -> playerName.equalsIgnoreCase(name) ? Optional.of(playerId) : Optional.empty();
            return new Fixture(new AdminTeleportCommand(spawnService, limitService, lookup), limitService);
        }
    }

    private static Map<String, CategoryConfig> categories() {
        return Map.of(
                "home", new CategoryConfig("home", OwnerKind.PLAYER, 3, CreationZone.TRUSTED_CLAIM, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false),
                "shop", new CategoryConfig("shop", OwnerKind.PLAYER, 1, CreationZone.TRUSTED_CLAIM, AccessMode.PUBLIC, VisibilityMode.LISTED, false, true, true)
        );
    }
}
