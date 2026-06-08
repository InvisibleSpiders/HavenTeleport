package com.nick.teleportlocations.location;

public record CreateLocationRequest(
        String category,
        OwnerRef owner,
        String name,
        SavedPosition position,
        AccessMode accessMode,
        VisibilityMode visibilityMode,
        CostSpec cost,
        boolean mainHome
) {
}
