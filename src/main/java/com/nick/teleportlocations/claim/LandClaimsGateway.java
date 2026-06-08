package com.nick.teleportlocations.claim;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.UUID;

public interface LandClaimsGateway {
    boolean available();

    boolean hasClaimAt(SavedPosition position);

    boolean canInteract(UUID playerId, SavedPosition position, String actionKey);

    default boolean canBuild(UUID playerId, SavedPosition position) {
        return canInteract(playerId, position, "build");
    }

    default boolean ownsClaimAt(UUID playerId, SavedPosition position) {
        return false;
    }

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

    static LandClaimsGateway fixedOwned(boolean hasClaim, boolean canInteract, boolean ownsClaim) {
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

            @Override
            public boolean canBuild(UUID playerId, SavedPosition position) {
                return canInteract;
            }

            @Override
            public boolean ownsClaimAt(UUID playerId, SavedPosition position) {
                return ownsClaim;
            }
        };
    }
}
