package com.nick.teleportlocations.elevator;

import java.util.Locale;

public enum ElevatorParticle {
    WAX_ON,
    END_ROD;

    public static ElevatorParticle parse(String value) {
        if (value == null || value.isBlank()) {
            return WAX_ON;
        }
        return ElevatorParticle.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
