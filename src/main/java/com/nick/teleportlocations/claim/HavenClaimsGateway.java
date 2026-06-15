package com.nick.teleportlocations.claim;

import com.nick.teleportlocations.location.SavedPosition;
import java.util.UUID;

public interface HavenClaimsGateway {
    boolean available();

    boolean hasClaimAt(SavedPosition position);

    boolean canInteract(UUID playerId, SavedPosition position, String actionKey);

    default boolean canBuild(UUID playerId, SavedPosition position) {
        return canInteract(playerId, position, "build");
    }

    default boolean ownsClaimAt(UUID playerId, SavedPosition position) {
        return false;
    }

    default boolean canVisitorsEnter(SavedPosition position) {
        return false;
    }

    static HavenClaimsGateway missing() {
        return new HavenClaimsGateway() {
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

    static HavenClaimsGateway fixed(boolean hasClaim, boolean canInteract) {
        return new HavenClaimsGateway() {
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

    static HavenClaimsGateway fixedOwned(boolean hasClaim, boolean canInteract, boolean ownsClaim) {
        return new HavenClaimsGateway() {
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

            @Override
            public boolean canVisitorsEnter(SavedPosition position) {
                return hasClaim && canInteract;
            }
        };
    }
}
