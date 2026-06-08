package com.nick.teleportlocations.location;

import java.time.Instant;
import java.util.UUID;

public record TeleportLocation(
        UUID id,
        String category,
        OwnerRef owner,
        String name,
        String normalizedName,
        SavedPosition position,
        AccessMode accessMode,
        VisibilityMode visibilityMode,
        CostSpec cost,
        boolean mainHome,
        Instant createdAt,
        Instant updatedAt
) {
    public static TeleportLocation create(
            UUID id,
            String category,
            OwnerRef owner,
            String name,
            SavedPosition position,
            AccessMode accessMode,
            VisibilityMode visibilityMode,
            CostSpec cost,
            boolean mainHome,
            Instant now
    ) {
        if (mainHome && !"home".equals(category)) {
            throw new LocationValidationException("Only homes can be marked as main homes.");
        }
        AccessMode finalAccess = accessMode;
        VisibilityMode finalVisibility = visibilityMode;
        CostSpec finalCost = cost;
        if ("shop".equals(category)) {
            finalAccess = AccessMode.PUBLIC;
            finalVisibility = VisibilityMode.LISTED;
            finalCost = CostSpec.free();
        }
        return new TeleportLocation(
                id,
                category,
                owner,
                name,
                LocationName.normalize(name),
                position,
                finalAccess,
                finalVisibility,
                finalCost,
                mainHome,
                now,
                now
        );
    }

    public TeleportLocation withMainHome(boolean newMainHome, Instant now) {
        if (newMainHome && !"home".equals(category)) {
            throw new LocationValidationException("Only homes can be marked as main homes.");
        }
        return new TeleportLocation(id, category, owner, name, normalizedName, position, accessMode, visibilityMode, cost, newMainHome, createdAt, now);
    }
}
