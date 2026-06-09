package com.nick.teleportlocations.teleport.effect;

import org.bukkit.entity.Player;

public final class NoOpTeleportEffectService implements TeleportEffectService {
    @Override
    public void departure(Player player) {
    }

    @Override
    public void arrival(Player player) {
    }

    @Override
    public void denied(Player player) {
    }
}
