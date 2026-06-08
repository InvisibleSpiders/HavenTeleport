package com.nick.teleportlocations.command;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class BukkitPlayerLookup implements PlayerLookup {
    @Override
    public Optional<UUID> find(String input) {
        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            return Optional.of(player.getUniqueId());
        }
    }
}
