package com.nick.teleportlocations.config;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;
import com.nick.teleportlocations.teleport.effect.TeleportEffectConfig;
import com.nick.teleportlocations.teleport.effect.TeleportEffectProfile;
import com.nick.teleportlocations.teleport.effect.TeleportSoundProfile;
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
                yaml.getString("teleport.inaccessible-destinations.mode", "mark"),
                loadTeleportEffects(yaml),
                yaml.getString("spawn.first-join.target", "spawn"),
                yaml.getString("spawn.login.target", "last-location"),
                yaml.getString("spawn.death-respawn.target", "main-home"),
                yaml.getStringList("spawn.death-respawn.fallback"),
                yaml.getInt("elevators.max-distance", 16),
                yaml.getInt("elevators.cooldown-seconds", 2),
                yaml.getInt("teleport-blocks.max-distance", 64),
                yaml.getInt("teleport-blocks.cooldown-seconds", 3),
                yaml.getBoolean("elevators.particles.enabled", true),
                yaml.getString("elevators.particles.default", "WAX_ON"),
                yaml.getInt("elevators.particles.interval-ticks", 20),
                yaml.getBoolean("tpa.enabled", true),
                yaml.getInt("tpa.request-timeout-seconds", 60),
                yaml.getInt("tpa.cooldown-seconds", 0),
                yaml.getInt("tpa.warmup-seconds", 0),
                yaml.getBoolean("tpa.cancel-warmup-on-move", true)
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

    private static TeleportEffectConfig loadTeleportEffects(YamlConfiguration yaml) {
        return new TeleportEffectConfig(
                yaml.getBoolean("teleport.effects.enabled", true),
                loadEffectProfile(yaml, "teleport.effects.departure", "PORTAL", null),
                loadEffectProfile(yaml, "teleport.effects.arrival", "REVERSE_PORTAL", null),
                loadEffectProfile(yaml, "teleport.effects.denied", "DUST", "RED")
        );
    }

    private static TeleportEffectProfile loadEffectProfile(
            YamlConfiguration yaml,
            String path,
            String defaultParticle,
            String defaultColor
    ) {
        return new TeleportEffectProfile(
                yaml.getBoolean(path + ".enabled", true),
                yaml.getString(path + ".particle", defaultParticle),
                yaml.getInt(path + ".count", 32),
                yaml.getDouble(path + ".radius", 0.45D),
                yaml.getDouble(path + ".y-offset", 1.0D),
                yaml.getString(path + ".color", defaultColor),
                (float) yaml.getDouble(path + ".size", 1.0D),
                loadSoundProfile(yaml, path + ".sound")
        );
    }

    private static TeleportSoundProfile loadSoundProfile(YamlConfiguration yaml, String path) {
        return new TeleportSoundProfile(
                yaml.getBoolean(path + ".enabled", false),
                yaml.getString(path + ".name", "ENTITY_ENDERMAN_TELEPORT"),
                (float) yaml.getDouble(path + ".volume", 0.5D),
                (float) yaml.getDouble(path + ".pitch", 1.0D),
                yaml.getString(path + ".audience", "SELF"),
                yaml.getDouble(path + ".radius", 8.0D)
        );
    }
}
