package com.nick.teleportlocations.claim;

import com.invisiblespiders.havenclaims.api.HavenClaimsApi;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicesManager;

public final class BukkitHavenClaimsGateway implements HavenClaimsGateway {
    private final HavenClaimsApi api;

    private BukkitHavenClaimsGateway(HavenClaimsApi api) {
        this.api = api;
    }

    public static HavenClaimsGateway discover(ServicesManager servicesManager) {
        HavenClaimsApi api = servicesManager.load(HavenClaimsApi.class);
        return api == null ? HavenClaimsGateway.missing() : new BukkitHavenClaimsGateway(api);
    }

    public static String createActionKey(String category) {
        return "teleportlocations.create." + category;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean hasClaimAt(SavedPosition position) {
        Location location = toLocation(position);
        return location != null && api.getClaimAt(location).isPresent();
    }

    @Override
    public boolean canInteract(UUID playerId, SavedPosition position, String actionKey) {
        Player player = Bukkit.getPlayer(playerId);
        Location location = toLocation(position);
        if (player == null || location == null) {
            return false;
        }
        return api.canInteract(player, location, actionKey);
    }

    @Override
    public boolean canBuild(UUID playerId, SavedPosition position) {
        Player player = Bukkit.getPlayer(playerId);
        Location location = toLocation(position);
        if (player == null || location == null) {
            return false;
        }
        return api.canBuild(player, location);
    }

    @Override
    public boolean ownsClaimAt(UUID playerId, SavedPosition position) {
        Location location = toLocation(position);
        if (location == null) {
            return false;
        }
        return api.getClaimAt(location)
                .map(claim -> playerId.equals(claim.ownerUuid()))
                .orElse(false);
    }

    @Override
    public boolean canVisitorsEnter(SavedPosition position) {
        Location location = toLocation(position);
        return location != null && api.canVisitorsEnter(location);
    }

    private static Location toLocation(SavedPosition position) {
        Objects.requireNonNull(position, "position");
        World world = Bukkit.getWorld(position.worldId());
        if (world == null) {
            world = Bukkit.getWorld(position.worldName());
        }
        if (world == null) {
            return null;
        }
        return new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
    }
}
