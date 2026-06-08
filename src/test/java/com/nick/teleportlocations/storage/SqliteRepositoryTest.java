package com.nick.teleportlocations.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SqliteRepositoryTest {
    @Test
    void savesAndLoadsLocation() throws Exception {
        Path db = Files.createTempFile("teleport-locations", ".db");
        Database database = Database.open(db);
        SqliteLocationRepository repository = new SqliteLocationRepository(database);
        TeleportLocation location = TeleportLocation.create(
                UUID.randomUUID(),
                "home",
                OwnerRef.player(UUID.randomUUID()),
                "base",
                new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                true,
                Instant.EPOCH
        );

        repository.save(location);

        assertThat(repository.findById(location.id())).contains(location);
    }

    @Test
    void savesAndLoadsLimitOverride() throws Exception {
        Path db = Files.createTempFile("teleport-locations", ".db");
        Database database = Database.open(db);
        SqliteLimitRepository repository = new SqliteLimitRepository(database);
        UUID playerId = UUID.randomUUID();

        repository.setLimit(playerId, "home", 9);

        assertThat(repository.findLimit(playerId, "home")).contains(9);
    }
}
