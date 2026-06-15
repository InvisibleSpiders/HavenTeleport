package com.nick.teleportlocations.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SqliteTeleportBlockRepositoryTest {
    @Test
    void savesLoadsUpdatesAndDeletesTeleportBlocks() throws Exception {
        Path db = Files.createTempFile("teleport-blocks", ".db");
        Database database = Database.open(db);
        SqliteTeleportBlockRepository repository = new SqliteTeleportBlockRepository(database);
        UUID worldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        TeleportBlock first = new TeleportBlock(
                UUID.randomUUID(),
                ownerId,
                new SavedPosition(worldId, "world", 4.0, 64.0, 7.0, 0.0f, 0.0f),
                Optional.empty(),
                Optional.empty(),
                Instant.EPOCH,
                Instant.EPOCH
        );
        TeleportBlock second = new TeleportBlock(
                UUID.randomUUID(),
                ownerId,
                new SavedPosition(worldId, "world", 10.0, 64.0, 7.0, 0.0f, 0.0f),
                Optional.of(first.id()),
                Optional.empty(),
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1)
        );

        repository.save(first);
        repository.save(second);
        repository.save(first.withLink(Optional.of(second.id()), Instant.EPOCH.plusSeconds(1)));

        assertThat(repository.findAt(worldId, 4, 64, 7))
                .map(TeleportBlock::linkedBlockId)
                .contains(Optional.of(second.id()));
        assertThat(repository.findById(second.id())).contains(second);
        assertThat(repository.findAll()).hasSize(2);

        repository.delete(first.id());

        assertThat(repository.findAt(worldId, 4, 64, 7)).isEmpty();
        assertThat(repository.findAll()).containsExactly(second);
    }
}
