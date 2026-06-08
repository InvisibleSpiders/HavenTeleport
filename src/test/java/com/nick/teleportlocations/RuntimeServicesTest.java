package com.nick.teleportlocations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RuntimeServicesTest {
    @Test
    void opensSqliteBackedServicesInDataFolder(@TempDir Path dataFolder) {
        try (RuntimeServices services = RuntimeServices.open(dataFolder)) {
            assertThat(Files.exists(dataFolder.resolve("locations.db"))).isTrue();
            assertThat(services.config().categories()).containsKey("home");
            assertThat(services.locationService()).isNotNull();
            assertThat(services.limitService()).isNotNull();
        }
    }
}
