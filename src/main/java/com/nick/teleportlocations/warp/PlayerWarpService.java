package com.nick.teleportlocations.warp;

import com.nick.teleportlocations.claim.ClaimAccess;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.CreateLocationRequest;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerWarpService {
    private static final String CATEGORY = "player_warp";

    private final LocationService locations;
    private final LimitService limits;
    private final CreationPolicyService creationPolicy;

    public PlayerWarpService(LocationService locations, LimitService limits, CreationPolicyService creationPolicy) {
        this.locations = locations;
        this.limits = limits;
        this.creationPolicy = creationPolicy;
    }

    public PlayerWarpResult setWarp(UUID ownerId, String name, SavedPosition position, boolean adminBypass) {
        ClaimAccess claimAccess = creationPolicy.canCreate(ownerId, CATEGORY, position, adminBypass);
        if (!claimAccess.allowed()) {
            return PlayerWarpResult.claimDenied(claimAccess.reason());
        }

        OwnerRef owner = OwnerRef.player(ownerId);
        Optional<TeleportLocation> existing = locations.find(owner, CATEGORY, name);
        List<TeleportLocation> ownerWarps = locations.list(owner, CATEGORY);
        if (existing.isEmpty() && reachedLimit(ownerId, ownerWarps.size())) {
            return PlayerWarpResult.limitReached();
        }

        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                owner,
                name,
                position,
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                false
        ));
        return existing.isPresent() ? PlayerWarpResult.updated(location) : PlayerWarpResult.created(location);
    }

    public Optional<TeleportLocation> resolveVisibleWarp(UUID viewerId, String name) {
        return visibleWarps(viewerId).stream()
                .filter(warp -> warp.normalizedName().equals(com.nick.teleportlocations.location.LocationName.normalize(name)))
                .findFirst();
    }

    public List<TeleportLocation> visibleWarps(UUID viewerId) {
        return locations.list(CATEGORY).stream()
                .filter(warp -> warp.owner().playerIdOptional().filter(viewerId::equals).isPresent()
                        || warp.visibilityMode() == VisibilityMode.LISTED && warp.accessMode() == AccessMode.PUBLIC)
                .sorted(Comparator.comparing(TeleportLocation::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<TeleportLocation> ownerWarps(UUID ownerId) {
        return locations.list(OwnerRef.player(ownerId), CATEGORY);
    }

    public PlayerWarpResult deleteWarp(UUID ownerId, String name) {
        Optional<TeleportLocation> warp = locations.find(OwnerRef.player(ownerId), CATEGORY, name);
        if (warp.isEmpty()) {
            return PlayerWarpResult.notFound();
        }
        locations.delete(warp.orElseThrow().id());
        return PlayerWarpResult.deleted();
    }

    public PlayerWarpResult setAccess(UUID ownerId, String name, AccessMode accessMode) {
        Optional<TeleportLocation> warp = locations.find(OwnerRef.player(ownerId), CATEGORY, name);
        if (warp.isEmpty()) {
            return PlayerWarpResult.notFound();
        }
        TeleportLocation existing = warp.orElseThrow();
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                OwnerRef.player(ownerId),
                existing.name(),
                existing.position(),
                accessMode,
                existing.visibilityMode(),
                existing.cost(),
                false
        ));
        return PlayerWarpResult.updated(location);
    }

    public PlayerWarpResult setVisibility(UUID ownerId, String name, VisibilityMode visibilityMode) {
        Optional<TeleportLocation> warp = locations.find(OwnerRef.player(ownerId), CATEGORY, name);
        if (warp.isEmpty()) {
            return PlayerWarpResult.notFound();
        }
        TeleportLocation existing = warp.orElseThrow();
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                OwnerRef.player(ownerId),
                existing.name(),
                existing.position(),
                existing.accessMode(),
                visibilityMode,
                existing.cost(),
                false
        ));
        return PlayerWarpResult.updated(location);
    }

    private boolean reachedLimit(UUID ownerId, int existingWarps) {
        int limit = limits.resolveLimit(ownerId, CATEGORY);
        return limit >= 0 && existingWarps >= limit;
    }
}
