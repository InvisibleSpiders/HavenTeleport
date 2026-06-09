package com.nick.teleportlocations.command;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitOnlinePlayerLookup implements OnlinePlayerLookup {
    @Override
    public Optional<Player> find(String input) {
        return Optional.ofNullable(Bukkit.getPlayerExact(input))
                .or(() -> Optional.ofNullable(Bukkit.getPlayer(input)));
    }

    @Override
    public Optional<Player> find(UUID playerId) {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }
}
