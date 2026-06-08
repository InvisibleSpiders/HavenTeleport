package com.nick.teleportlocations.config;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static PluginConfig fromResources() {
        YamlConfiguration yaml = loadResource("config.yml");
        return new PluginConfig(
                loadCategories(yaml.getConfigurationSection("categories")),
                yaml.getString("integrations.landclaims.missing-service-policy", "deny-claim-required"),
                yaml.getBoolean("integrations.vaultunlocked.treat-money-costs-as-free-when-missing", false),
                yaml.getInt("teleport.warmup-seconds", 3),
                yaml.getBoolean("teleport.cancel-on-move", true),
                yaml.getInt("teleport.safe-search-radius", 3),
                yaml.getString("spawn.first-join.target", "spawn"),
                yaml.getString("spawn.login.target", "last-location"),
                yaml.getString("spawn.death-respawn.target", "main-home"),
                yaml.getStringList("spawn.death-respawn.fallback")
        );
    }

    public static String resourceText(String name) {
        try (InputStream stream = ConfigLoader.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read resource " + name, exception);
        }
    }

    private static YamlConfiguration loadResource(String name) {
        InputStream stream = ConfigLoader.class.getClassLoader().getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalArgumentException("Missing resource " + name);
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static Map<String, CategoryConfig> loadCategories(ConfigurationSection section) {
        Map<String, CategoryConfig> categories = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection category = section.getConfigurationSection(key);
            categories.put(key, new CategoryConfig(
                    key,
                    OwnerKind.parse(category.getString("owner", "player")),
                    category.getInt("default-limit"),
                    CreationZone.parse(category.getString("creation-zone", "anywhere")),
                    AccessMode.parse(category.getString("default-access", "private")),
                    VisibilityMode.parse(category.getString("default-visibility", "hidden")),
                    category.getBoolean("allows-cost", false),
                    category.getBoolean("force-public", false),
                    category.getBoolean("force-listed", false)
            ));
        }
        return Map.copyOf(categories);
    }
}
