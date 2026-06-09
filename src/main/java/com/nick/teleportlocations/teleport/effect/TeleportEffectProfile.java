package com.nick.teleportlocations.teleport.effect;

public record TeleportEffectProfile(
        boolean enabled,
        String particle,
        int count,
        double radius,
        double yOffset,
        String particleColor,
        float dustSize,
        TeleportSoundProfile sound
) {
}
