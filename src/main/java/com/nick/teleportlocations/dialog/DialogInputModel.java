package com.nick.teleportlocations.dialog;

public record DialogInputModel(
        String key,
        String label,
        float min,
        float max,
        float step,
        float initial,
        String labelFormat
) {
}
