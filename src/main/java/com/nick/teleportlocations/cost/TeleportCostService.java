package com.nick.teleportlocations.cost;

import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.CostType;
import java.util.UUID;

public final class TeleportCostService {
    private final EconomyGateway economy;
    private final PlayerResourceGateway resources;
    private final boolean moneyFreeWhenEconomyMissing;

    public TeleportCostService(EconomyGateway economy, PlayerResourceGateway resources, boolean moneyFreeWhenEconomyMissing) {
        this.economy = economy;
        this.resources = resources;
        this.moneyFreeWhenEconomyMissing = moneyFreeWhenEconomyMissing;
    }

    public ChargeResult charge(UUID playerId, String category, CostSpec cost) {
        if ("shop".equals(category) && cost.type() != CostType.FREE) {
            return ChargeResult.failure("shop-free-only");
        }
        return switch (cost.type()) {
            case FREE -> ChargeResult.ok();
            case MONEY -> chargeMoney(playerId, cost.amount());
            case XP_LEVELS -> resources.takeXpLevels(playerId, (int) cost.amount()) ? ChargeResult.ok() : ChargeResult.failure("not-enough-xp");
            case XP_POINTS -> resources.takeXpPoints(playerId, (int) cost.amount()) ? ChargeResult.ok() : ChargeResult.failure("not-enough-xp");
            case ITEM -> resources.takeItem(playerId, cost.itemMaterial(), cost.itemAmount()) ? ChargeResult.ok() : ChargeResult.failure("not-enough-items");
        };
    }

    private ChargeResult chargeMoney(UUID playerId, double amount) {
        if (!economy.available()) {
            return moneyFreeWhenEconomyMissing ? ChargeResult.ok() : ChargeResult.failure("economy-missing");
        }
        return economy.withdraw(playerId, amount) ? ChargeResult.ok() : ChargeResult.failure("not-enough-money");
    }
}
