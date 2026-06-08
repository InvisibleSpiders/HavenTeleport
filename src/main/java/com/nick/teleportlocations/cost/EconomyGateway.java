package com.nick.teleportlocations.cost;

import java.util.UUID;

public interface EconomyGateway {
    boolean available();

    boolean withdraw(UUID playerId, double amount);

    static EconomyGateway unavailable() {
        return new EconomyGateway() {
            @Override
            public boolean available() {
                return false;
            }

            @Override
            public boolean withdraw(UUID playerId, double amount) {
                return false;
            }
        };
    }
}
