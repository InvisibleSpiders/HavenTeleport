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
    }
}
