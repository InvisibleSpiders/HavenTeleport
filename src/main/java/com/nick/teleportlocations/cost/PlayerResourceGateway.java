package com.nick.teleportlocations.cost;

import java.util.UUID;

public interface PlayerResourceGateway {
    boolean takeXpLevels(UUID playerId, int levels);

    boolean takeXpPoints(UUID playerId, int points);

    boolean takeItem(UUID playerId, String material, int amount);

    static PlayerResourceGateway empty() {
        return new PlayerResourceGateway() {
            @Override
            public boolean takeXpLevels(UUID playerId, int levels) {
                return false;
            }

            @Override
            public boolean takeXpPoints(UUID playerId, int points) {
                return false;
            }

            @Override
            public boolean takeItem(UUID playerId, String material, int amount) {
                return false;
            }
        };
    }
}
