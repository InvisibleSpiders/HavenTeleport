package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.Objects;
import java.util.UUID;

public final class TeleportAccessService {
    public static final String ENTER_ACTION = "teleportlocations.enter";

    private final LandClaimsGateway landClaims;

    public TeleportAccessService(LandClaimsGateway landClaims) {
        this.landClaims = Objects.requireNonNull(landClaims, "landClaims");
    }

    public TeleportAccessResult canEnter(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        if (adminBypassClaims || !landClaims.available() || !landClaims.hasClaimAt(position)) {
            return TeleportAccessResult.allow();
        }
        return landClaims.canInteract(playerId, position, ENTER_ACTION)
                ? TeleportAccessResult.allow()
                : TeleportAccessResult.deny("claim-entry-denied");
    }
}
