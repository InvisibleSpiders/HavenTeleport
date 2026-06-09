package com.nick.teleportlocations.teleport.effect;

public record TeleportEffectConfig(
        boolean enabled,
        TeleportEffectProfile departure,
        TeleportEffectProfile arrival,
        TeleportEffectProfile denied
) {
}
