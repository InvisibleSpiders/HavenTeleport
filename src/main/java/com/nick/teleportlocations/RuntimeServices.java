package com.nick.teleportlocations;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.claim.MissingLandClaimsPolicy;
import com.nick.teleportlocations.cost.BukkitPlayerResourceGateway;
import com.nick.teleportlocations.cost.EconomyGateway;
import com.nick.teleportlocations.cost.HavenEconomyGateway;
import com.nick.teleportlocations.cost.PlayerResourceGateway;
import com.nick.teleportlocations.cost.TeleportCostService;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.elevator.ElevatorRepository;
import com.nick.teleportlocations.elevator.ElevatorService;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.limit.LimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.outpost.OutpostService;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.spawn.SpawnPolicy;
import com.nick.teleportlocations.spawn.SpawnPolicyService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.spawn.SpawnTarget;
import com.nick.teleportlocations.storage.Database;
import com.nick.teleportlocations.storage.LocationRepository;
import com.nick.teleportlocations.storage.SqliteElevatorRepository;
import com.nick.teleportlocations.storage.SqliteLimitRepository;
import com.nick.teleportlocations.storage.SqliteLocationRepository;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import com.nick.teleportlocations.teleport.TeleportSafetyService;
import com.nick.teleportlocations.tpa.TeleportRequestService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import dev.invisiblespiders.haven.api.service.HavenDataSource;
import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RuntimeServices(
        PluginConfig config,
        Database database,
        LocationRepository locationRepository,
        LimitRepository limitRepository,
        ElevatorRepository elevatorRepository,
        AdminBypassService adminBypassService,
        LocationService locationService,
        LimitService limitService,
        EconomyGateway economyGateway,
        PlayerResourceGateway playerResourceGateway,
        TeleportCostService teleportCostService,
        TeleportChargeService teleportChargeService,
        TeleportSafetyService teleportSafetyService,
        TeleportRequestService teleportRequestService,
        CreationPolicyService creationPolicyService,
        HomeService homeService,
        PlayerWarpService playerWarpService,
        ShopWarpService shopWarpService,
        OutpostService outpostService,
        ServerWarpService serverWarpService,
        ElevatorService elevatorService,
        SpawnPolicyService spawnPolicyService,
        SpawnService spawnService
) implements AutoCloseable {
    public static final String PLUGIN_ID = "teleportlocations";
    public static final String MIGRATIONS_LOCATION = "db/migrations/teleportlocations";

    public RuntimeServices {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(locationRepository, "locationRepository");
        Objects.requireNonNull(limitRepository, "limitRepository");
        Objects.requireNonNull(elevatorRepository, "elevatorRepository");
        Objects.requireNonNull(adminBypassService, "adminBypassService");
        Objects.requireNonNull(locationService, "locationService");
        Objects.requireNonNull(limitService, "limitService");
        Objects.requireNonNull(economyGateway, "economyGateway");
        Objects.requireNonNull(playerResourceGateway, "playerResourceGateway");
        Objects.requireNonNull(teleportCostService, "teleportCostService");
        Objects.requireNonNull(teleportChargeService, "teleportChargeService");
        Objects.requireNonNull(teleportSafetyService, "teleportSafetyService");
        Objects.requireNonNull(teleportRequestService, "teleportRequestService");
        Objects.requireNonNull(creationPolicyService, "creationPolicyService");
        Objects.requireNonNull(homeService, "homeService");
        Objects.requireNonNull(playerWarpService, "playerWarpService");
        Objects.requireNonNull(shopWarpService, "shopWarpService");
        Objects.requireNonNull(outpostService, "outpostService");
        Objects.requireNonNull(serverWarpService, "serverWarpService");
        Objects.requireNonNull(elevatorService, "elevatorService");
        Objects.requireNonNull(spawnPolicyService, "spawnPolicyService");
        Objects.requireNonNull(spawnService, "spawnService");
    }

    public static RuntimeServices open(
            HavenDataSource dataSource,
            Optional<HavenEconomyService> economyService,
            LandClaimsGateway landClaims,
            ClassLoader classLoader
    ) {
        PluginConfig config = ConfigLoader.fromResources();
        dataSource.registerMigrations(PLUGIN_ID, MIGRATIONS_LOCATION, classLoader);
        Database database = Database.fromDataSource(dataSource.getDataSource());
        LocationRepository locations = new SqliteLocationRepository(database);
        LimitRepository limits = new SqliteLimitRepository(database);
        ElevatorRepository elevators = new SqliteElevatorRepository(database);
        AdminBypassService adminBypassService = new AdminBypassService();
        LimitService limitService = new LimitService(config.categories(), limits);
        LocationService locationService = new LocationService(locations, Instant::now);
        EconomyGateway economyGateway = economyService
                .<EconomyGateway>map(HavenEconomyGateway::new)
                .orElseGet(EconomyGateway::unavailable);
        PlayerResourceGateway playerResourceGateway = new BukkitPlayerResourceGateway();
        TeleportCostService teleportCostService = new TeleportCostService(
                economyGateway,
                playerResourceGateway,
                config.treatMoneyCostsAsFreeWhenEconomyMissing()
        );
        TeleportChargeService teleportChargeService = new TeleportChargeService(teleportCostService);
        TeleportSafetyService teleportSafetyService = new TeleportSafetyService();
        TeleportRequestService teleportRequestService = new TeleportRequestService(
                config.tpaRequestTimeoutSeconds(),
                config.tpaCooldownSeconds(),
                config.tpaMaxOutgoingRequests(),
                Instant::now
        );
        CreationPolicyService creationPolicyService = new CreationPolicyService(
                config.categories(),
                landClaims,
                MissingLandClaimsPolicy.parse(config.landClaimsMissingPolicy())
        );
        HomeService homeService = new HomeService(locationService, limitService, creationPolicyService);
        PlayerWarpService playerWarpService = new PlayerWarpService(locationService, limitService, creationPolicyService);
        ShopWarpService shopWarpService = new ShopWarpService(locationService, limitService, creationPolicyService);
        OutpostService outpostService = new OutpostService(locationService, limitService, creationPolicyService);
        ServerWarpService serverWarpService = new ServerWarpService(locationService);
        ElevatorService elevatorService = new ElevatorService(
                elevators,
                landClaims,
                ElevatorParticle.parse(config.elevatorDefaultParticle()),
                Instant::now
        );
        SpawnPolicyService spawnPolicyService = new SpawnPolicyService(spawnPolicy(config));
        SpawnService spawnService = new SpawnService(locationService, homeService);
        return new RuntimeServices(
                config,
                database,
                locations,
                limits,
                elevators,
                adminBypassService,
                locationService,
                limitService,
                economyGateway,
                playerResourceGateway,
                teleportCostService,
                teleportChargeService,
                teleportSafetyService,
                teleportRequestService,
                creationPolicyService,
                homeService,
                playerWarpService,
                shopWarpService,
                outpostService,
                serverWarpService,
                elevatorService,
                spawnPolicyService,
                spawnService
        );
    }

    private static SpawnPolicy spawnPolicy(PluginConfig config) {
        List<SpawnTarget> fallback = config.deathRespawnFallback().stream()
                .map(SpawnTarget::parse)
                .toList();
        return new SpawnPolicy(
                SpawnTarget.parse(config.firstJoinTarget()),
                SpawnTarget.parse(config.loginTarget()),
                SpawnTarget.parse(config.deathRespawnTarget()),
                fallback
        );
    }

    @Override
    public void close() {
        try {
            database.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not close TeleportLocations database", exception);
        }
    }
}
