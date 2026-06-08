package com.nick.teleportlocations.outpost;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OutpostService {
    private static final String CATEGORY = "outpost";

    private final LocationService locations;
    private final LimitService limits;
    private final CreationPolicyService creationPolicy;

    public OutpostService(LocationService locations, LimitService limits, CreationPolicyService creationPolicy) {
        this.locations = locations;
        this.limits = limits;
        this.creationPolicy = creationPolicy;
    }

    public OutpostResult setOutpost(UUID ownerId, String name, SavedPosition position, boolean adminBypass) {
        ClaimAccess claimAccess = creationPolicy.canCreate(ownerId, CATEGORY, position, adminBypass);
        if (!claimAccess.allowed()) {
            return OutpostResult.claimDenied(claimAccess.reason());
        }

        OwnerRef owner = OwnerRef.player(ownerId);
        Optional<TeleportLocation> existing = locations.find(owner, CATEGORY, name);
        List<TeleportLocation> ownerOutposts = locations.list(owner, CATEGORY);
        if (existing.isEmpty() && reachedLimit(ownerId, ownerOutposts.size())) {
            return OutpostResult.limitReached();
        }

        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                owner,
                name,
                position,
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                false
        ));
        return existing.isPresent() ? OutpostResult.updated(location) : OutpostResult.created(location);
    }

    public Optional<TeleportLocation> resolveOutpost(UUID ownerId, String name) {
        return locations.find(OwnerRef.player(ownerId), CATEGORY, name);
    }

    public List<TeleportLocation> listOutposts(UUID ownerId) {
        return locations.list(OwnerRef.player(ownerId), CATEGORY);
    }

    public OutpostResult deleteOutpost(UUID ownerId, String name) {
        Optional<TeleportLocation> outpost = resolveOutpost(ownerId, name);
        if (outpost.isEmpty()) {
            return OutpostResult.notFound();
        }
        locations.delete(outpost.orElseThrow().id());
        return OutpostResult.deleted();
    }

    private boolean reachedLimit(UUID ownerId, int existingOutposts) {
        int limit = limits.resolveLimit(ownerId, CATEGORY);
        return limit >= 0 && existingOutposts >= limit;
    }
}
