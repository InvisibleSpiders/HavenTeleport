package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.elevator.ElevatorRepository;
import com.nick.teleportlocations.location.SavedPosition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteElevatorRepository implements ElevatorRepository {
    private final Database database;

    public SqliteElevatorRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<ElevatorBlock> findAt(UUID worldId, int blockX, int blockY, int blockZ) {
        return queryOne(
                """
                SELECT * FROM teleport_elevator_blocks
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
    public List<ElevatorBlock> findColumn(UUID worldId, int blockX, int blockZ) {
        return queryMany(
                """
                SELECT * FROM teleport_elevator_blocks
                WHERE world_id = ? AND block_x = ? AND block_z = ?
                ORDER BY block_y
                """,
                statement -> {
                    statement.setString(1, worldId.toString());
                    statement.setInt(2, blockX);
                    statement.setInt(3, blockZ);
                }
        );
    }

    @Override
    public List<ElevatorBlock> findAll() {
        return queryMany(
                """
                SELECT * FROM teleport_elevator_blocks
                ORDER BY world_name, block_x, block_y, block_z
                """,
                statement -> {
                }
        );
    }

    @Override
    public void save(ElevatorBlock block) {
        try (Connection connection = database.connection()) {
            if (update(connection, block) == 0) {
                insert(connection, block);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save elevator block", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM teleport_elevator_blocks WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete elevator block", exception);
        }
    }

    private Optional<ElevatorBlock> queryOne(String sql, StatementBinder binder) {
        return queryMany(sql, binder).stream().findFirst();
    }

    private List<ElevatorBlock> queryMany(String sql, StatementBinder binder) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ElevatorBlock> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return List.copyOf(results);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not query elevator blocks", exception);
        }
    }

    private static int update(Connection connection, ElevatorBlock block) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE teleport_elevator_blocks
                SET owner_uuid = ?,
                    world_name = ?,
                    particle = ?,
                    updated_at = ?
                WHERE id = ?
                """)) {
            statement.setString(1, block.ownerId().toString());
            statement.setString(2, block.position().worldName());
            statement.setString(3, block.particle().name());
            statement.setString(4, block.updatedAt().toString());
            statement.setString(5, block.id().toString());
            return statement.executeUpdate();
        }
    }

    private static void insert(Connection connection, ElevatorBlock block) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO teleport_elevator_blocks(
                    id, owner_uuid, world_id, world_name, block_x, block_y, block_z,
                    particle, created_at, updated_at
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, block.id().toString());
            statement.setString(2, block.ownerId().toString());
            statement.setString(3, block.position().worldId().toString());
            statement.setString(4, block.position().worldName());
            statement.setInt(5, block.blockX());
            statement.setInt(6, block.blockY());
            statement.setInt(7, block.blockZ());
            statement.setString(8, block.particle().name());
            statement.setString(9, block.createdAt().toString());
            statement.setString(10, block.updatedAt().toString());
            statement.executeUpdate();
        }
    }

    private static ElevatorBlock map(ResultSet resultSet) throws SQLException {
        SavedPosition position = new SavedPosition(
                UUID.fromString(resultSet.getString("world_id")),
                resultSet.getString("world_name"),
                resultSet.getInt("block_x"),
                resultSet.getInt("block_y"),
                resultSet.getInt("block_z"),
                0.0f,
                0.0f
        );
        return new ElevatorBlock(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("owner_uuid")),
                position,
                ElevatorParticle.parse(resultSet.getString("particle")),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
