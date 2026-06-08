package com.nick.teleportlocations.location;

import java.util.UUID;

public record SavedPosition(
        UUID worldId,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}
