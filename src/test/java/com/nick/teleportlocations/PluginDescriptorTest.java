package com.nick.teleportlocations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class PluginDescriptorTest {
    @Test
    void htIsPrimaryAdminCommandWithLegacyAliases() throws IOException {
        String descriptor = new String(
                getClass().getClassLoader().getResourceAsStream("plugin.yml").readAllBytes(),
                StandardCharsets.UTF_8
        );

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
        assertThat(descriptor).contains("teleportlocations.elevator:");
        assertThat(descriptor).contains("teleportlocations.elevator.particle.end_rod:");
    }
}
