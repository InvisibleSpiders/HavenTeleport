package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.teleport.effect.TeleportEffectService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ManagedTeleportService {
    private final TeleportEffectService effects;
    private final Consumer<Runnable> mainThread;

    public ManagedTeleportService(TeleportEffectService effects, Consumer<Runnable> mainThread) {
        this.effects = Objects.requireNonNull(effects, "effects");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public CompletableFuture<Boolean> teleport(Player player, Location destination) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(destination, "destination");

        effects.departure(player);
        CompletableFuture<Boolean> teleport;
        try {
            teleport = player.teleportAsync(destination);
        } catch (RuntimeException exception) {
            effects.denied(player);
            return CompletableFuture.completedFuture(false);
        }
        if (teleport == null) {
            effects.denied(player);
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        teleport.whenComplete((success, throwable) -> mainThread.accept(() -> {
            boolean teleported = throwable == null && Boolean.TRUE.equals(success);
            if (teleported) {
                effects.arrival(player);
            } else {
                effects.denied(player);
            }
            result.complete(teleported);
        }));
        return result;
    }

    public void denied(Player player) {
        effects.denied(player);
    }
}
