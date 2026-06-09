package com.nick.teleportlocations.command;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface OnlinePlayerLookup {
    Optional<Player> find(String input);

    Optional<Player> find(UUID playerId);
}
