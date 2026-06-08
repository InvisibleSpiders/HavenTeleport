package com.nick.teleportlocations;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import dev.invisiblespiders.haven.api.service.HavenDataSource;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RuntimeServicesTest {
    @Test
    void opensHavenCoreBackedServicesAndRegistersMigrations(@TempDir Path dataFolder) {
        FakeHavenDataSource havenDataSource = new FakeHavenDataSource(dataFolder.resolve("locations.db"));

        try (RuntimeServices services = RuntimeServices.open(
                havenDataSource,
                Optional.empty(),
                LandClaimsGateway.fixed(true, true),
                getClass().getClassLoader()
        )) {
            assertThat(Files.exists(dataFolder.resolve("locations.db"))).isTrue();
            assertThat(havenDataSource.pluginId).isEqualTo("teleportlocations");
            assertThat(havenDataSource.location).isEqualTo("db/migrations/teleportlocations");
            assertThat(services.config().categories()).containsKey("home");
            assertThat(services.locationService()).isNotNull();
            assertThat(services.limitService()).isNotNull();
            assertThat(services.homeService()).isNotNull();
            assertThat(services.playerWarpService()).isNotNull();
            assertThat(services.shopWarpService()).isNotNull();
            assertThat(services.outpostService()).isNotNull();
            assertThat(services.serverWarpService()).isNotNull();
            assertThat(services.spawnService()).isNotNull();
            assertThat(services.spawnPolicyService().deathCandidates()).isNotEmpty();
        }
    }

    private static final class FakeHavenDataSource implements HavenDataSource {
        private final DataSource dataSource;
        private String pluginId;
        private String location;

        private FakeHavenDataSource(Path databasePath) {
            this.dataSource = new DriverManagerDataSource("jdbc:sqlite:" + databasePath.toAbsolutePath());
        }

        @Override
        public DataSource getDataSource() {
            return dataSource;
        }

        @Override
        public void registerMigrations(String pluginId, String location, ClassLoader loader) {
            this.pluginId = pluginId;
            this.location = location;
            DatabaseMigrationTestSupport.migrate(dataSource);
        }
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
