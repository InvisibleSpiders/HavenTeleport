package com.nick.teleportlocations.location;

public record CostSpec(CostType type, double amount, String itemMaterial, int itemAmount) {
    public static CostSpec free() {
        return new CostSpec(CostType.FREE, 0.0, "", 0);
    }

    public static CostSpec money(double amount) {
        return new CostSpec(CostType.MONEY, amount, "", 0);
    }

    public static CostSpec xpLevels(int levels) {
        return new CostSpec(CostType.XP_LEVELS, levels, "", 0);
    }

    public static CostSpec xpPoints(int points) {
        return new CostSpec(CostType.XP_POINTS, points, "", 0);
    }

    public static CostSpec item(String material, int amount) {
        return new CostSpec(CostType.ITEM, 0.0, material, amount);
    }
}
