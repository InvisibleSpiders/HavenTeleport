package com.nick.teleportlocations.location;

import java.util.Optional;
import java.util.UUID;

public record OwnerRef(OwnerType type, UUID playerId) {
    public static OwnerRef player(UUID playerId) {
        return new OwnerRef(OwnerType.PLAYER, playerId);
    }

    public static OwnerRef server() {
        return new OwnerRef(OwnerType.SERVER, null);
    }

    public Optional<UUID> playerIdOptional() {
        return Optional.ofNullable(playerId);
    }
}
