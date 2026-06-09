package com.nick.teleportlocations.teleport.effect;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class BukkitTeleportEffectService implements TeleportEffectService {
    private final TeleportEffectConfig config;
    private final Logger logger;
    private final Set<String> warnedKeys = new HashSet<>();

    public BukkitTeleportEffectService(TeleportEffectConfig config, Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void departure(Player player) {
        play(player, config.departure(), Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    @Override
    public void arrival(Player player) {
        play(player, config.arrival(), Particle.REVERSE_PORTAL, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void denied(Player player) {
        play(player, config.denied(), Particle.DUST, Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    private void play(Player player, TeleportEffectProfile profile, Particle fallbackParticle, Sound fallbackSound) {
        if (!config.enabled() || !profile.enabled()) {
            return;
        }
        Location location = player.getLocation().clone().add(0.0D, profile.yOffset(), 0.0D);
        spawnParticle(location, profile, fallbackParticle);
        playSound(player, location, profile.sound(), fallbackSound);
    }

    private void spawnParticle(Location location, TeleportEffectProfile profile, Particle fallbackParticle) {
        World world = location.getWorld();
        if (world == null || profile.count() <= 0) {
            return;
        }
        Particle particle = particle(profile.particle(), fallbackParticle);
        double radius = Math.max(0.0D, profile.radius());
        if (particle == Particle.DUST) {
            world.spawnParticle(
                    Particle.DUST,
                    location,
                    Math.max(1, profile.count()),
                    radius,
                    radius,
                    radius,
                    0.0D,
                    new Particle.DustOptions(color(profile.particleColor()), Math.max(0.1F, profile.dustSize()))
            );
            return;
        }
        world.spawnParticle(
                particle,
                location,
                Math.max(1, profile.count()),
                radius,
                radius,
                radius,
                0.0D
        );
    }

    private void playSound(Player player, Location location, TeleportSoundProfile profile, Sound fallbackSound) {
        if (!profile.enabled()) {
            return;
        }
        Sound sound = sound(profile.name(), fallbackSound);
        float volume = Math.max(0.0F, profile.volume());
        float pitch = Math.max(0.0F, profile.pitch());
        if ("NEARBY".equalsIgnoreCase(profile.audience())) {
            playNearby(player, location, sound, volume, pitch, profile.radius());
            return;
        }
        player.playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private static void playNearby(Player player, Location location, Sound sound, float volume, float pitch, double radius) {
        World world = location.getWorld();
        if (world == null) {
            player.playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
            return;
        }
        double maxDistanceSquared = Math.max(0.0D, radius) * Math.max(0.0D, radius);
        for (Player nearby : world.getPlayers()) {
            if (nearby.getLocation().distanceSquared(location) <= maxDistanceSquared) {
                nearby.playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
            }
        }
    }

    private Particle particle(String name, Particle fallback) {
        try {
            return Particle.valueOf(normalize(name));
        } catch (RuntimeException exception) {
            warnOnce("particle:" + name, "Unknown teleport particle '" + name + "', using " + fallback.name() + ".");
            return fallback;
        }
    }

    private Sound sound(String name, Sound fallback) {
        Sound resolved = Registry.SOUNDS.get(soundKey(name));
        if (resolved == null) {
            warnOnce("sound:" + name, "Unknown teleport sound '" + name + "', using fallback.");
            return fallback;
        }
        return resolved;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static NamespacedKey soundKey(String value) {
        String normalized = normalize(value);
        String key = switch (normalized) {
            case "BLOCK_AMETHYST_BLOCK_CHIME" -> "block.amethyst_block.chime";
            case "BLOCK_NOTE_BLOCK_BASS" -> "block.note_block.bass";
            case "ENTITY_ENDERMAN_TELEPORT" -> "entity.enderman.teleport";
            default -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        };
        if (key.contains(":")) {
            return NamespacedKey.fromString(key);
        }
        return NamespacedKey.minecraft(key);
    }

    private static Color color(String value) {
        return switch (normalize(value)) {
            case "AQUA" -> Color.AQUA;
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "FUCHSIA", "MAGENTA" -> Color.FUCHSIA;
            case "GRAY", "GREY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIME" -> Color.LIME;
            case "MAROON" -> Color.MAROON;
            case "NAVY" -> Color.NAVY;
            case "OLIVE" -> Color.OLIVE;
            case "ORANGE" -> Color.ORANGE;
            case "PURPLE" -> Color.PURPLE;
            case "SILVER" -> Color.SILVER;
            case "TEAL" -> Color.TEAL;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            case "RED" -> Color.RED;
            default -> Color.RED;
        };
    }

    private void warnOnce(String key, String message) {
        if (warnedKeys.add(key)) {
            logger.warning(message);
        }
    }
}
