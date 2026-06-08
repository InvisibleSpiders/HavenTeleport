package com.nick.teleportlocations.storage;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.CostType;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.OwnerType;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteLocationRepository implements LocationRepository {
    private final Database database;

    public SqliteLocationRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<TeleportLocation> findById(UUID id) {
        return queryOne("SELECT * FROM teleport_locations WHERE id = ?", statement -> statement.setString(1, id.toString()));
    }

    @Override
    public Optional<TeleportLocation> findByIdentity(OwnerRef owner, String category, String normalizedName) {
        return queryOne(
                """
                SELECT * FROM teleport_locations
                WHERE owner_type = ?
                  AND (owner_uuid = ? OR (owner_uuid IS NULL AND ? IS NULL))
                  AND category = ?
                  AND normalized_name = ?
                """,
                statement -> {
                    statement.setString(1, owner.type().name());
                    statement.setString(2, owner.playerId() == null ? null : owner.playerId().toString());
                    statement.setString(3, owner.playerId() == null ? null : owner.playerId().toString());
                    statement.setString(4, category);
                    statement.setString(5, normalizedName);
                }
        );
    }

    @Override
    public List<TeleportLocation> findByOwnerAndCategory(OwnerRef owner, String category) {
        return queryMany(
                """
                SELECT * FROM teleport_locations
                WHERE owner_type = ?
                  AND (owner_uuid = ? OR (owner_uuid IS NULL AND ? IS NULL))
                  AND category = ?
                ORDER BY name
                """,
                statement -> {
                    statement.setString(1, owner.type().name());
                    statement.setString(2, owner.playerId() == null ? null : owner.playerId().toString());
                    statement.setString(3, owner.playerId() == null ? null : owner.playerId().toString());
                    statement.setString(4, category);
                }
        );
    }

    @Override
    public List<TeleportLocation> findByCategory(String category) {
        return queryMany(
                "SELECT * FROM teleport_locations WHERE category = ? ORDER BY name",
                statement -> statement.setString(1, category)
        );
    }

    @Override
    public void save(TeleportLocation location) {
        try (Connection connection = database.connection()) {
            if (update(connection, location) == 0) {
                insert(connection, location);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save teleport location", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM teleport_locations WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete teleport location", exception);
        }
    }

    private Optional<TeleportLocation> queryOne(String sql, StatementBinder binder) {
        return queryMany(sql, binder).stream().findFirst();
    }

    private List<TeleportLocation> queryMany(String sql, StatementBinder binder) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TeleportLocation> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return List.copyOf(results);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not query teleport locations", exception);
        }
    }

    private static int update(Connection connection, TeleportLocation location) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE teleport_locations
                SET category = ?,
                    owner_type = ?,
                    owner_uuid = ?,
                    name = ?,
                    normalized_name = ?,
                    world_id = ?,
                    world_name = ?,
                    x = ?,
                    y = ?,
                    z = ?,
                    yaw = ?,
                    pitch = ?,
                    access_mode = ?,
                    visibility_mode = ?,
                    cost_type = ?,
                    cost_amount = ?,
                    cost_item_material = ?,
                    cost_item_amount = ?,
                    main_home = ?,
                    updated_at = ?
                WHERE id = ?
                """)) {
            bindLocationWithoutId(statement, location);
            statement.setString(21, location.id().toString());
            return statement.executeUpdate();
        }
    }

    private static void insert(Connection connection, TeleportLocation location) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO teleport_locations(
                    id, category, owner_type, owner_uuid, name, normalized_name,
                    world_id, world_name, x, y, z, yaw, pitch,
                    access_mode, visibility_mode, cost_type, cost_amount,
                    cost_item_material, cost_item_amount, main_home, created_at, updated_at
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            bindLocation(statement, location);
            statement.executeUpdate();
        }
    }

    private static void bindLocation(PreparedStatement statement, TeleportLocation location) throws SQLException {
        statement.setString(1, location.id().toString());
        statement.setString(2, location.category());
        statement.setString(3, location.owner().type().name());
        statement.setString(4, location.owner().playerId() == null ? null : location.owner().playerId().toString());
        statement.setString(5, location.name());
        statement.setString(6, location.normalizedName());
        statement.setString(7, location.position().worldId().toString());
        statement.setString(8, location.position().worldName());
        statement.setDouble(9, location.position().x());
        statement.setDouble(10, location.position().y());
        statement.setDouble(11, location.position().z());
        statement.setFloat(12, location.position().yaw());
        statement.setFloat(13, location.position().pitch());
        statement.setString(14, location.accessMode().name());
        statement.setString(15, location.visibilityMode().name());
        statement.setString(16, location.cost().type().name());
        statement.setDouble(17, location.cost().amount());
        statement.setString(18, location.cost().itemMaterial());
        statement.setInt(19, location.cost().itemAmount());
        statement.setInt(20, location.mainHome() ? 1 : 0);
        statement.setString(21, location.createdAt().toString());
        statement.setString(22, location.updatedAt().toString());
    }

    private static void bindLocationWithoutId(PreparedStatement statement, TeleportLocation location) throws SQLException {
        statement.setString(1, location.category());
        statement.setString(2, location.owner().type().name());
        statement.setString(3, location.owner().playerId() == null ? null : location.owner().playerId().toString());
        statement.setString(4, location.name());
        statement.setString(5, location.normalizedName());
        statement.setString(6, location.position().worldId().toString());
        statement.setString(7, location.position().worldName());
        statement.setDouble(8, location.position().x());
        statement.setDouble(9, location.position().y());
        statement.setDouble(10, location.position().z());
        statement.setFloat(11, location.position().yaw());
        statement.setFloat(12, location.position().pitch());
        statement.setString(13, location.accessMode().name());
        statement.setString(14, location.visibilityMode().name());
        statement.setString(15, location.cost().type().name());
        statement.setDouble(16, location.cost().amount());
        statement.setString(17, location.cost().itemMaterial());
        statement.setInt(18, location.cost().itemAmount());
        statement.setInt(19, location.mainHome() ? 1 : 0);
        statement.setString(20, location.updatedAt().toString());
    }

    private static TeleportLocation map(ResultSet resultSet) throws SQLException {
        UUID ownerUuid = resultSet.getString("owner_uuid") == null ? null : UUID.fromString(resultSet.getString("owner_uuid"));
        OwnerRef owner = OwnerType.valueOf(resultSet.getString("owner_type")) == OwnerType.SERVER
                ? OwnerRef.server()
                : OwnerRef.player(ownerUuid);
        SavedPosition position = new SavedPosition(
                UUID.fromString(resultSet.getString("world_id")),
                resultSet.getString("world_name"),
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getDouble("z"),
                resultSet.getFloat("yaw"),
                resultSet.getFloat("pitch")
        );
        CostSpec cost = new CostSpec(
                CostType.valueOf(resultSet.getString("cost_type")),
                resultSet.getDouble("cost_amount"),
                resultSet.getString("cost_item_material"),
                resultSet.getInt("cost_item_amount")
        );
        return new TeleportLocation(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("category"),
                owner,
                resultSet.getString("name"),
                resultSet.getString("normalized_name"),
                position,
                AccessMode.valueOf(resultSet.getString("access_mode")),
                VisibilityMode.valueOf(resultSet.getString("visibility_mode")),
                cost,
                resultSet.getInt("main_home") == 1,
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
