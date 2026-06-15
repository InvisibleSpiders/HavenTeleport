package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import com.nick.teleportlocations.teleportblock.TeleportBlockRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteTeleportBlockRepository implements TeleportBlockRepository {
    private final Database database;

    public SqliteTeleportBlockRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<TeleportBlock> findById(UUID id) {
        return queryOne("SELECT * FROM teleport_blocks WHERE id = ?", statement -> statement.setString(1, id.toString()));
    }

    @Override
    public Optional<TeleportBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ) {
        return queryOne(
                """
                SELECT * FROM teleport_blocks
                WHERE world_id = ? AND block_x = ? AND block_y = ? AND block_z = ?
                """,
                statement -> {
                    statement.setString(1, worldId.toString());
                    statement.setInt(2, blockX);
                    statement.setInt(3, blockY);
                    statement.setInt(4, blockZ);
                }
        );
    }

    @Override
    public List<TeleportBlock> findAll() {
        return queryMany("SELECT * FROM teleport_blocks", statement -> {
        });
    }

    @Override
    public void save(TeleportBlock block) {
        try (Connection connection = database.connection()) {
            if (update(connection, block) == 0) {
                insert(connection, block);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save teleport block", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM teleport_blocks WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete teleport block", exception);
        }
    }

    private Optional<TeleportBlock> queryOne(String sql, StatementBinder binder) {
        return queryMany(sql, binder).stream().findFirst();
    }

    private List<TeleportBlock> queryMany(String sql, StatementBinder binder) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TeleportBlock> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return List.copyOf(results);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not query teleport blocks", exception);
        }
    }

    private static int update(Connection connection, TeleportBlock block) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE teleport_blocks
                SET owner_uuid = ?,
                    world_name = ?,
                    linked_block_id = ?,
                    target_location_id = ?,
                    updated_at = ?
                WHERE id = ?
                """)) {
            statement.setString(1, block.ownerId().toString());
            statement.setString(2, block.position().worldName());
            statement.setString(3, block.linkedBlockId().map(UUID::toString).orElse(null));
            statement.setString(4, block.targetLocationId().map(UUID::toString).orElse(null));
            statement.setString(5, block.updatedAt().toString());
            statement.setString(6, block.id().toString());
            return statement.executeUpdate();
        }
    }

    private static void insert(Connection connection, TeleportBlock block) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO teleport_blocks(
                    id, owner_uuid, world_id, world_name, block_x, block_y, block_z,
                    linked_block_id, target_location_id, created_at, updated_at
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, block.id().toString());
            statement.setString(2, block.ownerId().toString());
            statement.setString(3, block.position().worldId().toString());
            statement.setString(4, block.position().worldName());
            statement.setInt(5, block.blockX());
            statement.setInt(6, block.blockY());
            statement.setInt(7, block.blockZ());
            statement.setString(8, block.linkedBlockId().map(UUID::toString).orElse(null));
            statement.setString(9, block.targetLocationId().map(UUID::toString).orElse(null));
            statement.setString(10, block.createdAt().toString());
            statement.setString(11, block.updatedAt().toString());
            statement.executeUpdate();
        }
    }

    private static TeleportBlock map(ResultSet resultSet) throws SQLException {
        SavedPosition position = new SavedPosition(
                UUID.fromString(resultSet.getString("world_id")),
                resultSet.getString("world_name"),
                resultSet.getInt("block_x"),
                resultSet.getInt("block_y"),
                resultSet.getInt("block_z"),
                0.0f,
                0.0f
        );
        String linkedBlockId = resultSet.getString("linked_block_id");
        String targetLocationId = resultSet.getString("target_location_id");
        return new TeleportBlock(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("owner_uuid")),
                position,
                linkedBlockId == null ? Optional.empty() : Optional.of(UUID.fromString(linkedBlockId)),
                targetLocationId == null ? Optional.empty() : Optional.of(UUID.fromString(targetLocationId)),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
