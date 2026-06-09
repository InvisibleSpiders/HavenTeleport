package com.nick.teleportlocations.storage;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class Database implements AutoCloseable {
    private final DataSource dataSource;
    private final boolean migrateOnOpen;

    private Database(DataSource dataSource, boolean migrateOnOpen) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.migrateOnOpen = migrateOnOpen;
    }

    public static Database open(Path path) {
        Database database = new Database(new DriverManagerDataSource("jdbc:sqlite:" + path.toAbsolutePath()), true);
        database.migrate();
        return database;
    }

    public static Database fromDataSource(DataSource dataSource) {
        return new Database(dataSource, false);
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    private void migrate() {
        if (!migrateOnOpen) {
            return;
        }
        try {
            migrate(connection());
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not open SQLite database", exception);
        }
    }

    private static void migrate(Connection connection) throws SQLException {
        try (connection; Statement statement = connection.createStatement()) {
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
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teleport_elevator_blocks (
                        id TEXT PRIMARY KEY,
                        owner_uuid TEXT NOT NULL,
                        world_id TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        block_x INTEGER NOT NULL,
                        block_y INTEGER NOT NULL,
                        block_z INTEGER NOT NULL,
                        particle TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        CONSTRAINT teleport_elevator_blocks_position UNIQUE(world_id, block_x, block_y, block_z)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS teleport_elevator_blocks_column
                    ON teleport_elevator_blocks(world_id, block_x, block_z)
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS teleport_blocks (
                        id TEXT PRIMARY KEY,
                        owner_uuid TEXT NOT NULL,
                        world_id TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        block_x INTEGER NOT NULL,
                        block_y INTEGER NOT NULL,
                        block_z INTEGER NOT NULL,
                        linked_block_id TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        CONSTRAINT teleport_blocks_position UNIQUE(world_id, block_x, block_y, block_z)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS teleport_blocks_link
                    ON teleport_blocks(linked_block_id)
                    """);
        }
    }

    @Override
    public void close() {
    }

    private record DriverManagerDataSource(String url) implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
