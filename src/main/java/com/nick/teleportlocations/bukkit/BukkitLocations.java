package com.nick.teleportlocations.bukkit;

import com.nick.teleportlocations.location.SavedPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class BukkitLocations {
    private BukkitLocations() {
    }

    public static SavedPosition save(Location location) {
        World world = location.getWorld();
        return new SavedPosition(
                world.getUID(),
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Location load(SavedPosition position) {
        World world = Bukkit.getWorld(position.worldId());
        if (world == null) {
            world = Bukkit.getWorld(position.worldName());
        }
        return world == null ? null : new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
    }
}
