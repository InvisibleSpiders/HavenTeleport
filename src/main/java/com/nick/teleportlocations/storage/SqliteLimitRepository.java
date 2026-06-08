package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.limit.LimitRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class SqliteLimitRepository implements LimitRepository {
    private final Database database;

    public SqliteLimitRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<Integer> findLimit(UUID playerId, String category) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                SELECT limit_amount
                FROM teleport_location_limits
                WHERE player_uuid = ? AND category = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, category);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("limit_amount"));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find player limit", exception);
        }
    }

    @Override
    public void setLimit(UUID playerId, String category, int limit) {
        try (Connection connection = database.connection()) {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE teleport_location_limits
                    SET limit_amount = ?
                    WHERE player_uuid = ? AND category = ?
                    """)) {
                update.setInt(1, limit);
                update.setString(2, playerId.toString());
                update.setString(3, category);
                if (update.executeUpdate() > 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO teleport_location_limits(player_uuid, category, limit_amount)
                    VALUES(?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, category);
                insert.setInt(3, limit);
                insert.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save player limit", exception);
        }
    }

    @Override
    public void clearLimit(UUID playerId, String category) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM teleport_location_limits
                WHERE player_uuid = ? AND category = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, category);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear player limit", exception);
        }
    }
}
