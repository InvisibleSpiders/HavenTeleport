package com.nick.teleportlocations.claim;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.UUID;

public interface LandClaimsGateway {
    boolean available();

    boolean hasClaimAt(SavedPosition position);

    boolean canInteract(UUID playerId, SavedPosition position, String actionKey);

    static LandClaimsGateway missing() {
        return new LandClaimsGateway() {
            @Override
            public boolean available() {
                return false;
            }

            @Override
            public boolean hasClaimAt(SavedPosition position) {
                return false;
            }

            @Override
            public boolean canInteract(UUID playerId, SavedPosition position, String actionKey) {
                return false;
            }
        };
    }

    static LandClaimsGateway fixed(boolean hasClaim, boolean canInteract) {
        return new LandClaimsGateway() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public boolean hasClaimAt(SavedPosition position) {
                return hasClaim;
            }

            @Override
            public boolean canInteract(UUID playerId, SavedPosition position, String actionKey) {
                return canInteract;
            }
        };
    }
}
