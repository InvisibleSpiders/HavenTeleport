package com.nick.teleportlocations.teleport;

public record TeleportSafetyResult(boolean safe, String reason) {
    public static TeleportSafetyResult ok() {
        return new TeleportSafetyResult(true, "safe");
    }

    public static TeleportSafetyResult unsafe(String reason) {
        return new TeleportSafetyResult(false, reason);
    }
}
