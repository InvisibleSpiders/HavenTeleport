package com.nick.teleportlocations.upgrade;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import dev.invisiblespiders.haven.api.upgrade.UpgradeContext;
import dev.invisiblespiders.haven.api.upgrade.UpgradeRequirement;
import dev.invisiblespiders.haven.api.upgrade.UpgradeRequirementResult;

class MoneyRequirement implements UpgradeRequirement {

    private final HavenEconomyService economy;
    private final double cost;

    MoneyRequirement(HavenEconomyService economy, double cost) {
        this.economy = economy;
        this.cost = cost;
    }

    @Override
    public String type() {
        return "money";
    }

    @Override
    public UpgradeRequirementResult validate(UpgradeContext context) {
        if (!economy.has(context.targetPlayerId(), cost)) {
            String formatted = economy.format(cost);
            return UpgradeRequirementResult.failure(
                    "insufficient-funds",
                    "You need " + formatted + " to purchase this upgrade."
            );
        }
        return UpgradeRequirementResult.success();
    }

    @Override
    public void consume(UpgradeContext context) {
        economy.withdraw(context.targetPlayerId(), cost);
    }

    @Override
    public void refund(UpgradeContext context) {
        economy.deposit(context.targetPlayerId(), cost);
    }
}
