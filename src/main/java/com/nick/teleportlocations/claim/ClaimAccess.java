package com.nick.teleportlocations.claim;

public record ClaimAccess(boolean allowed, String reason) {
    public static ClaimAccess allow() {
        return new ClaimAccess(true, "allowed");
    }

    public static ClaimAccess deny(String reason) {
        return new ClaimAccess(false, reason);
    }
}
