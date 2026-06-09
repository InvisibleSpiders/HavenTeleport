package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.spawn.SpawnPolicyService;
import com.nick.teleportlocations.spawn.SpawnService;
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

    public SpawnListener(SpawnService spawn, SpawnPolicyService policy, TeleportSafetyService safety) {
        this.spawn = spawn;
        this.policy = policy;
        this.safety = safety;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.firstJoinCandidates());
        target.ifPresent(location -> {
            if (!safety.validate(location.position()).safe()) {
                event.getPlayer().sendMessage(Component.text("Configured first-join spawn is unsafe; default spawn was used.", NamedTextColor.RED));
                return;
            }
            Location destination = BukkitLocations.load(location.position());
            if (destination == null) {
                event.getPlayer().sendMessage(Component.text("Configured first-join spawn world is not loaded; default spawn was used.", NamedTextColor.RED));
                return;
            }
            event.getPlayer().teleportAsync(destination);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<TeleportLocation> target = spawn.resolve(event.getPlayer().getUniqueId(), policy.deathCandidates());
        target.ifPresent(location -> {
            if (!safety.validate(location.position()).safe()) {
                event.getPlayer().sendMessage(Component.text("Configured respawn target is unsafe; default spawn was used.", NamedTextColor.RED));
                return;
            }
            Location destination = BukkitLocations.load(location.position());
            if (destination == null) {
                event.getPlayer().sendMessage(Component.text("Configured respawn world is not loaded; default spawn was used.", NamedTextColor.RED));
                return;
            }
            event.setRespawnLocation(destination);
        });
    }
}
