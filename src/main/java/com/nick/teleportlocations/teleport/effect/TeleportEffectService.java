package com.nick.teleportlocations.teleport.effect;

import org.bukkit.entity.Player;

public interface TeleportEffectService {
    void departure(Player player);

    void arrival(Player player);

    void denied(Player player);
}
