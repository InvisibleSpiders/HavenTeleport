package com.nick.teleportlocations.serverwarp;

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

public final class ServerWarpService {
    private static final String CATEGORY = "server_warp";

    private final LocationService locations;

    public ServerWarpService(LocationService locations) {
        this.locations = locations;
    }

    public ServerWarpResult setWarp(String name, SavedPosition position) {
        Optional<TeleportLocation> existing = locations.find(OwnerRef.server(), CATEGORY, name);
        TeleportLocation location = locations.createOrUpdate(new CreateLocationRequest(
                CATEGORY,
                OwnerRef.server(),
                name,
                position,
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                false
        ));
        return existing.isPresent() ? ServerWarpResult.updated(location) : ServerWarpResult.created(location);
    }

    public Optional<TeleportLocation> resolveVisibleWarp(String name) {
        return visibleWarps().stream()
                .filter(warp -> warp.normalizedName().equals(LocationName.normalize(name)))
                .findFirst();
    }

    public List<TeleportLocation> visibleWarps() {
        return locations.list(OwnerRef.server(), CATEGORY).stream()
                .filter(warp -> warp.visibilityMode() == VisibilityMode.LISTED)
                .filter(warp -> warp.accessMode() == AccessMode.PUBLIC)
                .sorted(Comparator.comparing(TeleportLocation::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ServerWarpResult deleteWarp(String name) {
        Optional<TeleportLocation> warp = locations.find(OwnerRef.server(), CATEGORY, name);
        if (warp.isEmpty()) {
            return ServerWarpResult.notFound();
        }
        locations.delete(warp.orElseThrow().id());
        return ServerWarpResult.deleted();
    }
}
