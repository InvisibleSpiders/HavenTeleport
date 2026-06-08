package com.nick.teleportlocations.access;

import com.nick.teleportlocations.claim.ClaimAccess;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.OwnerType;
import com.nick.teleportlocations.location.TeleportLocation;

public final class AccessService {
    public ClaimAccess canUse(TeleportLocation location, AccessContext context) {
        if (context.admin()) {
            return ClaimAccess.allow();
        }
        if (location.owner().type() == OwnerType.PLAYER && location.owner().playerIdOptional().filter(context.viewerId()::equals).isPresent()) {
            return ClaimAccess.allow();
        }
        if (location.accessMode() == AccessMode.PUBLIC) {
            return ClaimAccess.allow();
        }
        if (location.accessMode() == AccessMode.TRUSTED && context.trustedAtLocation()) {
            return ClaimAccess.allow();
        }
        return ClaimAccess.deny("access-denied");
    }
}
