package com.nick.teleportlocations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class PluginDescriptorTest {
    @Test
    void htIsPrimaryAdminCommandWithLegacyAliases() throws IOException {
        String descriptor = new String(
                getClass().getClassLoader().getResourceAsStream("plugin.yml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(descriptor).contains("name: HavenTeleport");
        assertThat(descriptor).contains("  ht:");
        assertThat(descriptor).contains("aliases: [haventeleport, tl]");
        assertThat(descriptor).contains("  tpa:");
        assertThat(descriptor).contains("  tpahere:");
        assertThat(descriptor).contains("  tpaccept:");
        assertThat(descriptor).contains("  tpdecline:");
        assertThat(descriptor).contains("  tpcancel:");
        assertThat(descriptor).contains("  tptoggle:");
        assertThat(descriptor).contains("  deloutpost:");
        assertThat(descriptor).contains("teleportlocations.tpa:");
        assertThat(descriptor).contains("teleportlocations.tpahere:");
        assertThat(descriptor).contains("teleportlocations.tpaccept:");
        assertThat(descriptor).contains("teleportlocations.tpdecline:");
        assertThat(descriptor).contains("teleportlocations.tpcancel:");
        assertThat(descriptor).contains("teleportlocations.tptoggle:");
        assertThat(descriptor).contains("teleportlocations.teleportblock:");
        assertThat(descriptor).contains("teleportlocations.teleportblock.link:");
        assertThat(descriptor).contains("teleportlocations.admin.teleportblock:");
        assertThat(descriptor).contains("teleportlocations.elevator:");
        assertThat(descriptor).contains("teleportlocations.elevator.particle.end_rod:");
        assertThat(descriptor).contains("  - HavenClaims");
        assertThat(descriptor).doesNotContain("HavenClaimsLegacy");
    }

    @Test
    void buildUsesPublishedHavenApisInsteadOfCheckedInJars() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));

        assertThat(build).contains("dev.invisiblespiders.haven:haven-api");
        assertThat(build).contains("com.invisiblespiders:havenclaims-api");
        assertThat(build).doesNotContain("libs/haven-api.jar");
        assertThat(build).doesNotContain("libs/havenclaims-api.jar");
    }

    @Test
    void buildProducesHavenTeleportArtifact() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));
        String workflow = Files.readString(Path.of(".github/workflows/build.yml"));

        assertThat(build).contains("archiveBaseName.set(\"HavenTeleport\")");
        assertThat(workflow).contains("name: HavenTeleport");
        assertThat(workflow).contains("path: build/libs/HavenTeleport-*.jar");
        assertThat(build).doesNotContain("archiveBaseName.set(\"TeleportLocations\")");
    }
}
