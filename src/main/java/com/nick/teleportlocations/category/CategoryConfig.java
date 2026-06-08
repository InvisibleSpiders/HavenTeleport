package com.nick.teleportlocations.category;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;

public record CategoryConfig(
        String key,
        OwnerKind owner,
        int defaultLimit,
        CreationZone creationZone,
        AccessMode defaultAccess,
        VisibilityMode defaultVisibility,
        boolean allowsCost,
        boolean forcePublic,
        boolean forceListed
) {
    public boolean unlimited() {
        return defaultLimit < 0;
    }
}
