package com.nick.teleportlocations.elevator.bukkit;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.elevator.ElevatorRepository;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class ElevatorParticleTask implements Runnable {
    private final ElevatorRepository repository;

    public ElevatorParticleTask(ElevatorRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void run() {
        for (ElevatorBlock block : repository.findAll()) {
            Location location = BukkitLocations.load(block.position());
            if (location == null || location.getWorld() == null) {
                continue;
            }
            location.getWorld().spawnParticle(
                    particle(block.particle()),
                    location.clone().add(0.5, 1.05, 0.5),
                    3,
                    0.24,
                    0.04,
                    0.24,
                    0.0
            );
        }
    }

    private static Particle particle(ElevatorParticle particle) {
        return switch (particle) {
            case END_ROD -> Particle.END_ROD;
            case WAX_ON -> Particle.WAX_ON;
        };
    }
}
