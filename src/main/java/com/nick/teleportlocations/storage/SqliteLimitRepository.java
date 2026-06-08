package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.limit.LimitRepository;
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
        try (PreparedStatement statement = database.connection().prepareStatement("""
                SELECT limit_amount
                FROM player_limits
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
        try (PreparedStatement statement = database.connection().prepareStatement("""
                INSERT INTO player_limits(player_uuid, category, limit_amount)
                VALUES(?, ?, ?)
                ON CONFLICT(player_uuid, category) DO UPDATE SET limit_amount = excluded.limit_amount
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, category);
            statement.setInt(3, limit);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save player limit", exception);
        }
    }

    @Override
    public void clearLimit(UUID playerId, String category) {
        try (PreparedStatement statement = database.connection().prepareStatement("""
                DELETE FROM player_limits
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
