package com.nick.teleportlocations.command;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface PlayerLookup {
    Optional<UUID> find(String input);
}
