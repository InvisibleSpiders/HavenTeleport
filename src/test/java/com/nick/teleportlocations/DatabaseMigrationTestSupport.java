package com.nick.teleportlocations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class DatabaseMigrationTestSupport {
    private DatabaseMigrationTestSupport() {
    }

    public static void migrate(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
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
                        updated_at TEXT NOT NULL,
                        CONSTRAINT teleport_locations_identity UNIQUE (owner_type, owner_uuid, category, normalized_name)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teleport_location_limits (
                        player_uuid TEXT NOT NULL,
                        category TEXT NOT NULL,
                        limit_amount INTEGER NOT NULL,
                        PRIMARY KEY(player_uuid, category)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not migrate test database", exception);
        }
    }
}
