package com.nick.teleportlocations.cost;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import java.util.UUID;

public final class HavenEconomyGateway implements EconomyGateway {
    private final HavenEconomyService economy;

    public HavenEconomyGateway(HavenEconomyService economy) {
        this.economy = economy;
    }

    @Override
    public boolean available() {
        return economy.isMoneyAvailable();
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        return economy.isMoneyAvailable() && economy.withdraw(playerId, amount);
    }
}
