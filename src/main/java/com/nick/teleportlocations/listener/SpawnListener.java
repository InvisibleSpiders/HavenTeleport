package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.spawn.SpawnPolicyService;
import com.nick.teleportlocations.spawn.SpawnService;
import com.nick.teleportlocations.teleport.ManagedTeleportService;
import com.nick.teleportlocations.teleport.TeleportSafetyService;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class SpawnListener implements Listener {
    private final SpawnService spawn;
    private final SpawnPolicyService policy;
    private final TeleportSafetyService safety;
    private final ManagedTeleportService managedTeleports;

    public SpawnListener(SpawnService spawn, SpawnPolicyService policy, TeleportSafetyService safety, ManagedTeleportService managedTeleports) {
        this.spawn = spawn;
        this.policy = policy;
        this.safety = safety;
        this.managedTeleports = managedTeleports;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.firstJoinCandidates());
        target.ifPresent(location -> {
            if (!safety.validate(location.position()).safe()) {
                managedTeleports.denied(event.getPlayer());
                event.getPlayer().sendMessage(Component.text("Configured first-join spawn is unsafe; default spawn was used.", NamedTextColor.RED));
                return;
            }
            Location destination = BukkitLocations.load(location.position());
            if (destination == null) {
                managedTeleports.denied(event.getPlayer());
                event.getPlayer().sendMessage(Component.text("Configured first-join spawn world is not loaded; default spawn was used.", NamedTextColor.RED));
                return;
            }
            managedTeleports.teleport(event.getPlayer(), destination);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.deathCandidates());
        target.ifPresent(location -> {
            if (!safety.validate(location.position()).safe()) {
                managedTeleports.denied(event.getPlayer());
                event.getPlayer().sendMessage(Component.text("Configured respawn target is unsafe; default spawn was used.", NamedTextColor.RED));
                return;
            }
            Location destination = BukkitLocations.load(location.position());
            if (destination == null) {
                managedTeleports.denied(event.getPlayer());
                event.getPlayer().sendMessage(Component.text("Configured respawn world is not loaded; default spawn was used.", NamedTextColor.RED));
                return;
            }
            event.setRespawnLocation(destination);
        });
    }
}
