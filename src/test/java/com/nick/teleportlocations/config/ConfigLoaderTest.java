package com.nick.teleportlocations.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;
import org.junit.jupiter.api.Test;

final class ConfigLoaderTest {
    @Test
    void loadsDefaultCategoriesAndShopInvariants() {
        PluginConfig config = ConfigLoader.fromResources();

        assertThat(config.categories()).containsKeys("home", "player_warp", "shop", "outpost", "server_warp");
        assertThat(config.categories().get("home").creationZone()).isEqualTo(CreationZone.TRUSTED_CLAIM);
        assertThat(config.categories().get("shop").allowsCost()).isFalse();
        assertThat(config.categories().get("shop").forcePublic()).isTrue();
        assertThat(config.categories().get("shop").forceListed()).isTrue();
        assertThat(config.categories().get("shop").defaultAccess()).isEqualTo(AccessMode.PUBLIC);
        assertThat(config.categories().get("shop").defaultVisibility()).isEqualTo(VisibilityMode.LISTED);
    }

    @Test
    void defaultYamlFilesAreCommented() {
        assertThat(ConfigLoader.resourceText("config.yml").lines()
                .filter(line -> line.stripLeading().startsWith("#"))
                .count()).isGreaterThanOrEqualTo(12);
        assertThat(ConfigLoader.resourceText("messages.yml")).contains("prefix:");
    }
}
