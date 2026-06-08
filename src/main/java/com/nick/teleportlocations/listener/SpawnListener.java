package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.spawn.SpawnPolicyService;
import com.nick.teleportlocations.spawn.SpawnService;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class SpawnListener implements Listener {
    private final SpawnService spawn;
    private final SpawnPolicyService policy;

    public SpawnListener(SpawnService spawn, SpawnPolicyService policy) {
        this.spawn = spawn;
        this.policy = policy;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.firstJoinCandidates());
        target.map(TeleportLocation::position)
                .map(BukkitLocations::load)
                .ifPresent(location -> event.getPlayer().teleportAsync(location));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.deathCandidates());
        Location location = target.map(TeleportLocation::position)
                .map(BukkitLocations::load)
                .orElse(null);
        if (location != null) {
            event.setRespawnLocation(location);
        }
    }
}
