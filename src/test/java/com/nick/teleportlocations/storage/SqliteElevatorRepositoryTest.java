package com.nick.teleportlocations.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.location.SavedPosition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SqliteElevatorRepositoryTest {
    @Test
    void savesLoadsAndDeletesElevatorBlocksByPositionAndColumn() throws Exception {
        Path db = Files.createTempFile("teleport-elevators", ".db");
        Database database = Database.open(db);
        SqliteElevatorRepository repository = new SqliteElevatorRepository(database);
        UUID worldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ElevatorBlock lower = new ElevatorBlock(
                UUID.randomUUID(),
                ownerId,
                new SavedPosition(worldId, "world", 4.0, 64.0, 7.0, 0.0f, 0.0f),
                ElevatorParticle.WAX_ON,
                Instant.EPOCH,
                Instant.EPOCH
        );
        ElevatorBlock upper = new ElevatorBlock(
                UUID.randomUUID(),
                ownerId,
                new SavedPosition(worldId, "world", 4.0, 70.0, 7.0, 0.0f, 0.0f),
                ElevatorParticle.END_ROD,
                Instant.EPOCH,
                Instant.EPOCH
        );

        repository.save(lower);
        repository.save(upper);

        assertThat(repository.findAt(worldId, 4, 64, 7)).contains(lower);
        assertThat(repository.findColumn(worldId, 4, 7)).containsExactly(lower, upper);
        assertThat(repository.findAll()).containsExactly(lower, upper);

        repository.delete(lower.id());

        assertThat(repository.findAt(worldId, 4, 64, 7)).isEmpty();
        assertThat(repository.findColumn(worldId, 4, 7)).containsExactly(upper);
    }
}
