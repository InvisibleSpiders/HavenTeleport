package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.Objects;
import java.util.UUID;

public final class TeleportAccessService {
    public static final String ENTER_ACTION = "teleportlocations.enter";

    private final HavenClaimsGateway havenClaims;

    public TeleportAccessService(HavenClaimsGateway havenClaims) {
        this.havenClaims = Objects.requireNonNull(havenClaims, "havenClaims");
    }

    public TeleportAccessResult canEnter(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        if (adminBypassClaims || !havenClaims.available() || !havenClaims.hasClaimAt(position)) {
            return TeleportAccessResult.allow();
        }
        return havenClaims.canInteract(playerId, position, ENTER_ACTION)
                ? TeleportAccessResult.allow()
                : TeleportAccessResult.deny("claim-entry-denied");
    }
}
