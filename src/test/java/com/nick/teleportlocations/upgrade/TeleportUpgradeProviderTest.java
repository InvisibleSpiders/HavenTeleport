package com.nick.teleportlocations.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import dev.invisiblespiders.haven.api.upgrade.UpgradeDefinition;
import dev.invisiblespiders.haven.api.upgrade.UpgradeLevel;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class TeleportUpgradeProviderTest {

    private static final String CONFIG =
            "upgrades:\n"
            + "  home-slots:\n"
            + "    names: [\"Home Slots I\", \"Home Slots II\", \"Home Slots III\", \"Home Slots IV\", \"Home Slots V\"]\n"
            + "    costs: [10000.0, 25000.0, 50000.0, 100000.0, 200000.0]\n"
            + "  warp-slots:\n"
            + "    names: [\"Warp Slots I\", \"Warp Slots II\", \"Warp Slots III\", \"Warp Slots IV\", \"Warp Slots V\"]\n"
            + "    costs: [10000.0, 25000.0, 50000.0, 100000.0, 200000.0]\n"
            + "  shop-slots:\n"
            + "    names: [\"Shop Slots I\", \"Shop Slots II\", \"Shop Slots III\", \"Shop Slots IV\", \"Shop Slots V\"]\n"
            + "    costs: [10000.0, 25000.0, 50000.0, 100000.0, 200000.0]\n";

    private static ConfigurationSection load(String yaml) {
        YamlConfiguration config = parseYaml(yaml);
        return config.getConfigurationSection("upgrades");
    }

    private static TeleportUpgradeProvider providerFromResource() {
        YamlConfiguration yaml = loadResourceYaml("config.yml");
        ConfigurationSection upgrades = yaml.getConfigurationSection("upgrades");
        return new TeleportUpgradeProvider(upgrades, null);
    }

    private static YamlConfiguration loadResourceYaml(String name) {
        InputStream stream = TeleportUpgradeProviderTest.class.getClassLoader().getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalArgumentException("Missing resource: " + name);
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static YamlConfiguration parseYaml(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse YAML", e);
        }
        return config;
    }

    @Test
    void providerIdAndDisplayName() {
        TeleportUpgradeProvider provider = providerFromResource();

        assertThat(provider.id()).isEqualTo("haven-teleport");
        assertThat(provider.displayName()).isEqualTo("Teleport");
    }

    @Test
    void registersPersonalCategory() {
        TeleportUpgradeProvider provider = providerFromResource();

        assertThat(provider.categories()).hasSize(1);
        assertThat(provider.categories().get(0).id()).isEqualTo("personal");
    }

    @Test
    void loadsThreeDefinitions() {
        TeleportUpgradeProvider provider = providerFromResource();

        List<UpgradeDefinition> defs = provider.definitions();
        assertThat(defs).hasSize(3);
        assertThat(defs).extracting(UpgradeDefinition::id)
                .containsExactlyInAnyOrder("home-slots", "warp-slots", "shop-slots");
    }

    @Test
    void eachDefinitionHasFiveLevels() {
        TeleportUpgradeProvider provider = providerFromResource();

        for (UpgradeDefinition def : provider.definitions()) {
            assertThat(def.levels())
                    .as("definition %s should have 5 levels", def.id())
                    .hasSize(5);
        }
    }

    @Test
    void levelNumbersAreSequential() {
        TeleportUpgradeProvider provider = providerFromResource();

        for (UpgradeDefinition def : provider.definitions()) {
            List<UpgradeLevel> levels = def.levels();
            for (int i = 0; i < levels.size(); i++) {
                assertThat(levels.get(i).level())
                        .as("level index %d in definition %s", i, def.id())
                        .isEqualTo(i + 1);
            }
        }
    }

    @Test
    void effectFactoryAlwaysEmpty() {
        TeleportUpgradeProvider provider = providerFromResource();

        assertThat(provider.effect("money", java.util.Map.of())).isEmpty();
        assertThat(provider.effect("unknown-type", java.util.Map.of())).isEmpty();
    }

    @Test
    void emptyConfigProducesNoDefinitions() {
        YamlConfiguration empty = parseYaml("upgrades: {}");
        ConfigurationSection section = empty.getConfigurationSection("upgrades");
        TeleportUpgradeProvider provider = new TeleportUpgradeProvider(section, null);

        assertThat(provider.definitions()).isEmpty();
    }

    @Test
    void levelHasNoRequirementsWhenEconomyIsNull() {
        TeleportUpgradeProvider provider = providerFromResource();

        for (UpgradeDefinition def : provider.definitions()) {
            for (UpgradeLevel level : def.levels()) {
                assertThat(level.requirements())
                        .as("level %d of %s should have no requirements when economy is null",
                                level.level(), def.id())
                        .isEmpty();
            }
        }
    }

    @Test
    void levelHasMoneyRequirementWhenEconomyIsPresent() {
        HavenEconomyService mockEconomy = mock(HavenEconomyService.class);
        TeleportUpgradeProvider p = new TeleportUpgradeProvider(load(CONFIG), mockEconomy);
        for (UpgradeDefinition def : p.definitions()) {
            for (UpgradeLevel level : def.levels()) {
                assertThat(level.requirements()).hasSize(1)
                        .as("Level %d of %s should have a money requirement", level.level(), def.id());
                assertThat(level.requirements().get(0).type()).isEqualTo("money");
            }
        }
    }
}
