package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.tpa.TeleportWarmupService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ScheduledTeleportService {
    private final TeleportWarmupService warmups;
    private final ManagedTeleportService teleports;

    public ScheduledTeleportService(TeleportWarmupService warmups, ManagedTeleportService teleports) {
        this.warmups = Objects.requireNonNull(warmups, "warmups");
        this.teleports = Objects.requireNonNull(teleports, "teleports");
    }

    public void teleport(Player player, Location destination) {
        warmups.begin(player, () -> teleports.teleport(player, destination));
    }
}
