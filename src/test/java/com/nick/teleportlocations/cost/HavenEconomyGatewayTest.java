package com.nick.teleportlocations.cost;

import static org.assertj.core.api.Assertions.assertThat;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class HavenEconomyGatewayTest {
    @Test
    void withdrawsThroughHavenEconomyWhenMoneyIsAvailable() {
        FakeEconomy economy = new FakeEconomy(true);
        HavenEconomyGateway gateway = new HavenEconomyGateway(economy);
        UUID playerId = UUID.randomUUID();

        assertThat(gateway.available()).isTrue();
        assertThat(gateway.withdraw(playerId, 25.0)).isTrue();
        assertThat(economy.lastPlayerId).isEqualTo(playerId);
        assertThat(economy.lastAmount).isEqualTo(25.0);
    }

    @Test
    void deniesMoneyCostsWhenHavenEconomyIsUnavailable() {
        HavenEconomyGateway gateway = new HavenEconomyGateway(new FakeEconomy(false));

        assertThat(gateway.available()).isFalse();
        assertThat(gateway.withdraw(UUID.randomUUID(), 25.0)).isFalse();
    }

    private static final class FakeEconomy implements HavenEconomyService {
        private final boolean available;
        private UUID lastPlayerId;
        private double lastAmount;

        private FakeEconomy(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isMoneyAvailable() {
            return available;
        }

        @Override
        public boolean isItemAvailable() {
            return false;
        }

        @Override
        public double getBalance(UUID uuid) {
            return 0;
        }

        @Override
        public boolean withdraw(UUID uuid, double amount) {
            this.lastPlayerId = uuid;
            this.lastAmount = amount;
            return available;
        }

        @Override
        public void deposit(UUID uuid, double amount) {
        }

        @Override
        public boolean has(UUID uuid, double amount) {
            return available;
        }

        @Override
        public String format(double amount) {
            return Double.toString(amount);
        }

        @Override
        public int getItemBalance(Player player) {
            return 0;
        }

        @Override
        public int withdrawItems(Player player, int count) {
            return 0;
        }

        @Override
        public void depositItems(Player player, int count) {
        }
    }
}
