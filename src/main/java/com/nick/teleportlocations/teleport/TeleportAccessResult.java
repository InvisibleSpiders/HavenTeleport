package com.nick.teleportlocations.teleport;

public record TeleportAccessResult(boolean allowed, String reason) {
    public static TeleportAccessResult allow() {
        return new TeleportAccessResult(true, "");
    }

    public static TeleportAccessResult deny(String reason) {
        return new TeleportAccessResult(false, reason);
    }
}
