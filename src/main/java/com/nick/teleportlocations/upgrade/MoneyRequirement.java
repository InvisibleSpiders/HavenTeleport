package com.nick.teleportlocations.upgrade;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import dev.invisiblespiders.haven.api.upgrade.UpgradeContext;
import dev.invisiblespiders.haven.api.upgrade.UpgradeRequirement;
import dev.invisiblespiders.haven.api.upgrade.UpgradeRequirementResult;
import java.util.Objects;
import java.util.logging.Logger;

class MoneyRequirement implements UpgradeRequirement {

    private static final Logger LOGGER = Logger.getLogger(MoneyRequirement.class.getName());

    private final HavenEconomyService economy;
    private final double cost;

    MoneyRequirement(HavenEconomyService economy, double cost) {
        this.economy = Objects.requireNonNull(economy, "economy");
        if (cost < 0) throw new IllegalArgumentException("amount cannot be negative: " + cost);
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
        boolean success = economy.withdraw(context.targetPlayerId(), cost);
        if (!success) {
            throw new IllegalStateException("money withdrawal failed for amount: " + cost);
        }
    }

    @Override
    public void refund(UpgradeContext context) {
        boolean success = economy.deposit(context.targetPlayerId(), cost);
        if (!success) {
            LOGGER.warning("money deposit (refund) failed for amount: " + cost
                    + " — player " + context.targetPlayerId() + " may have lost funds");
        }
    }
}
