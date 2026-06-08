package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.Set;

public final class TeleportSafetyService {
    private final Set<String> availableWorlds;

    public TeleportSafetyService(Set<String> availableWorlds) {
        this.availableWorlds = Set.copyOf(availableWorlds);
    }

    public TeleportSafetyResult validate(SavedPosition position) {
        if (!availableWorlds.contains(position.worldName())) {
            return TeleportSafetyResult.unsafe("missing-world");
        }
        if (position.y() < -64.0 || position.y() > 400.0) {
            return TeleportSafetyResult.unsafe("unsafe-height");
        }
        return TeleportSafetyResult.ok();
    }
}
