package com.nick.teleportlocations.shop;

import com.nick.teleportlocations.claim.ClaimAccess;
import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.CreateLocationRequest;
import com.nick.teleportlocations.location.LocationName;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ShopWarpService {
    private static final String CATEGORY = "shop";

    private final LocationService locations;
    private final LimitService limits;
    private final CreationPolicyService creationPolicy;

    public ShopWarpService(LocationService locations, LimitService limits, CreationPolicyService creationPolicy) {
        this.locations = locations;
        this.limits = limits;
        this.creationPolicy = creationPolicy;
    }

    public ShopWarpResult setShop(UUID ownerId, String name, SavedPosition position, boolean adminBypass) {
        ClaimAccess claimAccess = creationPolicy.canCreate(ownerId, CATEGORY, position, adminBypass);
        if (!claimAccess.allowed()) {
            return ShopWarpResult.claimDenied(claimAccess.reason());
        }

        OwnerRef owner = OwnerRef.player(ownerId);
        Optional<TeleportLocation> existing = locations.find(owner, CATEGORY, name);
        List<TeleportLocation> ownerShops = locations.list(owner, CATEGORY);
        if (existing.isEmpty() && reachedLimit(ownerId, ownerShops.size())) {
            return ShopWarpResult.limitReached();
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
        return existing.isPresent() ? ShopWarpResult.updated(location) : ShopWarpResult.created(location);
    }

    public Optional<TeleportLocation> resolveVisibleShop(UUID viewerId, String name) {
        return visibleShops(viewerId).stream()
                .filter(shop -> shop.normalizedName().equals(LocationName.normalize(name)))
                .findFirst();
    }

    public List<TeleportLocation> visibleShops(UUID viewerId) {
        return locations.list(CATEGORY).stream()
                .filter(shop -> shop.visibilityMode() == VisibilityMode.LISTED)
                .filter(shop -> shop.accessMode() == AccessMode.PUBLIC
                        || shop.owner().playerIdOptional().filter(viewerId::equals).isPresent())
                .sorted(Comparator.comparing(TeleportLocation::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<TeleportLocation> ownerShops(UUID ownerId) {
        return locations.list(OwnerRef.player(ownerId), CATEGORY);
    }

    public ShopWarpResult deleteShop(UUID ownerId, String name) {
        Optional<TeleportLocation> shop = locations.find(OwnerRef.player(ownerId), CATEGORY, name);
        if (shop.isEmpty()) {
            return ShopWarpResult.notFound();
        }
        locations.delete(shop.orElseThrow().id());
        return ShopWarpResult.deleted();
    }

    private boolean reachedLimit(UUID ownerId, int existingShops) {
        int limit = limits.resolveLimit(ownerId, CATEGORY);
        return limit >= 0 && existingShops >= limit;
    }
}
