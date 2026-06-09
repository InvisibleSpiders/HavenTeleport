package com.nick.teleportlocations.teleport.effect;

public record TeleportSoundProfile(
        boolean enabled,
        String name,
        float volume,
        float pitch,
        String audience,
        double radius
) {
}
