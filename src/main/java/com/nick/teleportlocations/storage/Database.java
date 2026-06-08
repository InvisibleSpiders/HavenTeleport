package com.nick.teleportlocations.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private final Connection connection;

    private Database(Connection connection) {
        this.connection = connection;
    }

    public static Database open(Path path) {
        try {
            Database database = new Database(DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath()));
            database.migrate();
            return database;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not open SQLite database", exception);
        }
    }

    public Connection connection() {
        return connection;
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teleport_locations (
                        id TEXT PRIMARY KEY,
                        category TEXT NOT NULL,
                        owner_type TEXT NOT NULL,
                        owner_uuid TEXT,
                        name TEXT NOT NULL,
                        normalized_name TEXT NOT NULL,
                        world_id TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        access_mode TEXT NOT NULL,
                        visibility_mode TEXT NOT NULL,
                        cost_type TEXT NOT NULL,
                        cost_amount REAL NOT NULL,
                        cost_item_material TEXT NOT NULL,
                        cost_item_amount INTEGER NOT NULL,
                        main_home INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS teleport_locations_identity
                    ON teleport_locations(owner_type, owner_uuid, category, normalized_name)
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_limits (
                        player_uuid TEXT NOT NULL,
                        category TEXT NOT NULL,
                        limit_amount INTEGER NOT NULL,
                        PRIMARY KEY(player_uuid, category)
                    )
                    """);
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
