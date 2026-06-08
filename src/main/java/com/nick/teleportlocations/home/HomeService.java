package com.nick.teleportlocations.home;

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

public final class HomeService {
    private static final String CATEGORY = "home";

    private final LocationService locations;
    private final LimitService limits;
    private final CreationPolicyService creationPolicy;

    public HomeService(LocationService locations, LimitService limits, CreationPolicyService creationPolicy) {
        this.locations = locations;
        this.limits = limits;
        this.creationPolicy = creationPolicy;
    }

    public HomeResult setHome(UUID playerId, String name, SavedPosition position, boolean adminBypass) {
        ClaimAccess claimAccess = creationPolicy.canCreate(playerId, CATEGORY, position, adminBypass);
        if (!claimAccess.allowed()) {
            return HomeResult.claimDenied(claimAccess.reason());
        }

        OwnerRef owner = OwnerRef.player(playerId);
        Optional<TeleportLocation> existing = locations.find(owner, CATEGORY, name);
        List<TeleportLocation> homes = locations.list(owner, CATEGORY);
        if (existing.isEmpty() && reachedLimit(playerId, homes.size())) {
            return HomeResult.limitReached();
        }

        boolean mainHome = existing.map(TeleportLocation::mainHome).orElse(homes.isEmpty());
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                owner,
                name,
                position,
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                mainHome
        ));
        return existing.isPresent() ? HomeResult.updated(location) : HomeResult.created(location);
    }

    public Optional<TeleportLocation> resolveHome(UUID playerId, String name) {
        if (name == null || name.isBlank()) {
            return locations.mainHome(playerId);
        }
        return locations.find(OwnerRef.player(playerId), CATEGORY, name);
    }

    public List<TeleportLocation> listHomes(UUID playerId) {
        return locations.list(OwnerRef.player(playerId), CATEGORY);
    }

    public HomeResult setMainHome(UUID playerId, String name) {
        Optional<TeleportLocation> home = resolveHome(playerId, name);
        if (home.isEmpty()) {
            return HomeResult.notFound();
        }
        TeleportLocation existing = home.orElseThrow();
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                OwnerRef.player(playerId),
                existing.name(),
                existing.position(),
                existing.accessMode(),
                existing.visibilityMode(),
                existing.cost(),
                true
        ));
        return HomeResult.updated(location);
    }

    public HomeResult deleteHome(UUID playerId, String name) {
        Optional<TeleportLocation> home = resolveHome(playerId, name);
        if (home.isEmpty()) {
            return HomeResult.notFound();
        }
        locations.delete(home.orElseThrow().id());
        return HomeResult.deleted();
    }

    private boolean reachedLimit(UUID playerId, int existingHomes) {
        int limit = limits.resolveLimit(playerId, CATEGORY);
        return limit >= 0 && existingHomes >= limit;
    }
}
