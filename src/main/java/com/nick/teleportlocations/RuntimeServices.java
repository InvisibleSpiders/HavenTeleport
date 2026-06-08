package com.nick.teleportlocations;

import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.config.PluginConfig;
import com.nick.teleportlocations.limit.LimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.storage.Database;
import com.nick.teleportlocations.storage.LocationRepository;
import com.nick.teleportlocations.storage.SqliteLimitRepository;
import com.nick.teleportlocations.storage.SqliteLocationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record RuntimeServices(
        PluginConfig config,
        Database database,
        LocationRepository locationRepository,
        LimitRepository limitRepository,
        LocationService locationService,
        LimitService limitService
) implements AutoCloseable {
    public RuntimeServices {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(locationRepository, "locationRepository");
        Objects.requireNonNull(limitRepository, "limitRepository");
        Objects.requireNonNull(locationService, "locationService");
        Objects.requireNonNull(limitService, "limitService");
    }

    public static RuntimeServices open(Path dataFolder) {
        try {
            Files.createDirectories(dataFolder);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create TeleportLocations data folder", exception);
        }

        PluginConfig config = ConfigLoader.fromResources();
        Database database = Database.open(dataFolder.resolve("locations.db"));
        LocationRepository locations = new SqliteLocationRepository(database);
        LimitRepository limits = new SqliteLimitRepository(database);
        LimitService limitService = new LimitService(config.categories(), limits);
        LocationService locationService = new LocationService(locations, Instant::now);
        return new RuntimeServices(config, database, locations, limits, locationService, limitService);
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
