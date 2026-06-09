package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.cost.EconomyGateway;
import com.nick.teleportlocations.cost.PlayerResourceGateway;
import com.nick.teleportlocations.cost.TeleportCostService;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.teleport.TeleportAccessService;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import com.nick.teleportlocations.teleport.TeleportSafetyService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class PlayerLocationCommandTest {
    @Test
    void convertsBukkitLocationToSavedPosition() {
        UUID worldId = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");

        SavedPosition position = BukkitLocations.save(
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

    @Test
    void setWarpCommandCreatesPlayerWarpAtCurrentLocation() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        World world = world("world");
        Player player = playerAt(playerId, new Location(world, 1.0, 64.0, 2.0, 90.0f, 10.0f));
        Command command = command("setwarp");

        fixture.command.onCommand(player, command, "setwarp", new String[] {"market"});

        assertThat(fixture.warps.ownerWarps(playerId)).extracting("name").containsExactly("market");
    }

    @Test
    void setShopCommandCreatesShopWarpAtCurrentLocation() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        World world = world("world");
        Player player = playerAt(playerId, new Location(world, 1.0, 64.0, 2.0, 90.0f, 10.0f));
        Command command = command("setshop");

        fixture.command.onCommand(player, command, "setshop", new String[] {"tools"});

        assertThat(fixture.shops.ownerShops(playerId)).extracting("name").containsExactly("tools");
    }

    @Test
    void setOutpostCommandCreatesOutpostAtCurrentLocation() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        World world = world("world");
        Player player = playerAt(playerId, new Location(world, 1.0, 64.0, 2.0, 90.0f, 10.0f));
        Command command = command("setoutpost");

        fixture.command.onCommand(player, command, "setoutpost", new String[] {"camp"});

        assertThat(fixture.outposts.listOutposts(playerId)).extracting("name").containsExactly("camp");
    }

    @Test
    void delOutpostCommandDeletesOwnOutpost() {
        Fixture fixture = Fixture.create();
        UUID playerId = UUID.randomUUID();
        World world = world("world");
        Player player = playerAt(playerId, new Location(world, 1.0, 64.0, 2.0, 90.0f, 10.0f));
        fixture.command.onCommand(player, command("setoutpost"), "setoutpost", new String[] {"camp"});

        fixture.command.onCommand(player, command("deloutpost"), "deloutpost", new String[] {"camp"});

        assertThat(fixture.outposts.listOutposts(playerId)).isEmpty();
    }

    private static Player playerAt(UUID playerId, Location location) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(location);
        when(player.hasPermission("teleportlocations.admin.bypass.creation")).thenReturn(false);
        return player;
    }

    private static World world(String name) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn(name);
        return world;
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private record Fixture(PlayerLocationCommand command, PlayerWarpService warps, ShopWarpService shops, OutpostService outposts) {
        private static Fixture create() {
            PluginConfig config = ConfigLoader.fromResources();
            InMemoryLocationRepository locations = new InMemoryLocationRepository();
            LocationService locationService = new LocationService(locations, () -> Instant.EPOCH);
            LimitService limitService = new LimitService(config.categories(), new InMemoryLimitRepository());
            CreationPolicyService creationPolicy = new CreationPolicyService(
                    config.categories(),
                    LandClaimsGateway.fixed(false, true),
                    MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED
            );
            HomeService homeService = new HomeService(locationService, limitService, creationPolicy);
            PlayerWarpService warpService = new PlayerWarpService(locationService, limitService, creationPolicy);
            ShopWarpService shopService = new ShopWarpService(locationService, limitService, creationPolicy);
            OutpostService outpostService = new OutpostService(locationService, limitService, creationPolicy);
            ServerWarpService serverWarpService = new ServerWarpService(locationService);
            SpawnService spawnService = new SpawnService(locationService, homeService);
            TeleportChargeService chargeService = new TeleportChargeService(
                    new TeleportCostService(EconomyGateway.unavailable(), PlayerResourceGateway.empty(), true)
            );
            return new Fixture(
                    new PlayerLocationCommand(
                            homeService,
                            warpService,
                            shopService,
                            outpostService,
                            serverWarpService,
                            spawnService,
                            chargeService,
                            new TeleportAccessService(LandClaimsGateway.fixed(false, true)),
                            new TeleportSafetyService(),
                            new com.nick.teleportlocations.admin.AdminBypassService(),
                            new DialogMenuService(),
                            new PaperDialogPresenter(),
                            false
                    ),
                    warpService,
                    shopService,
                    outpostService
            );
        }
    }
}
