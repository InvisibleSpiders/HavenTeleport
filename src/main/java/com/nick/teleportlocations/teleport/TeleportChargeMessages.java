package com.nick.teleportlocations.teleport;

public final class TeleportChargeMessages {
    private TeleportChargeMessages() {
    }

    public static String failure(String reason) {
        return switch (reason) {
            case "not-enough-money" -> "You do not have enough money for that teleport.";
            case "economy-missing" -> "Money-cost teleports are unavailable right now.";
            case "not-enough-xp" -> "You do not have enough experience for that teleport.";
            case "not-enough-items" -> "You do not have the required items for that teleport.";
            case "shop-free-only" -> "Shop warps must remain free.";
            default -> "You cannot pay for that teleport.";
        };
    }
}
