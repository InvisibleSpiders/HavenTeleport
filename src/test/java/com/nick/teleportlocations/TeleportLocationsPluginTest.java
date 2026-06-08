package com.nick.teleportlocations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TeleportLocationsPluginTest {
    @Test
    void pluginClassUsesSeparatePackage() {
        assertThat(TeleportLocationsPlugin.class.getName())
                .isEqualTo("com.nick.teleportlocations.TeleportLocationsPlugin");
    }
}
