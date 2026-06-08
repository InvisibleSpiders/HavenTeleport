package com.nick.teleportlocations.cost;

public record ChargeResult(boolean success, String reason) {
    public static ChargeResult ok() {
        return new ChargeResult(true, "charged");
    }

    public static ChargeResult failure(String reason) {
        return new ChargeResult(false, reason);
    }
}
