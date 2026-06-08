package com.nick.teleportlocations.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.cost.ChargeResult;
import com.nick.teleportlocations.cost.EconomyGateway;
import com.nick.teleportlocations.cost.PlayerResourceGateway;
import com.nick.teleportlocations.cost.TeleportCostService;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportChargeServiceTest {
    @Test
    void chargesVisitorForPaidPlayerWarp() {
        RecordingEconomy economy = new RecordingEconomy(true);
        TeleportChargeService service = new TeleportChargeService(new TeleportCostService(economy, PlayerResourceGateway.empty(), false));
        UUID visitor = UUID.randomUUID();

        ChargeResult result = service.chargeIfNeeded(visitor, false, playerWarp(UUID.randomUUID(), CostSpec.money(12.5)));

        assertThat(result.success()).isTrue();
        assertThat(economy.withdrawnPlayer).isEqualTo(visitor);
        assertThat(economy.withdrawnAmount).isEqualTo(12.5);
    }

    @Test
    void deniesTeleportWhenVisitorCannotPay() {
        RecordingEconomy economy = new RecordingEconomy(false);
        TeleportChargeService service = new TeleportChargeService(new TeleportCostService(economy, PlayerResourceGateway.empty(), false));

        ChargeResult result = service.chargeIfNeeded(UUID.randomUUID(), false, playerWarp(UUID.randomUUID(), CostSpec.money(12.5)));

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("not-enough-money");
    }

    @Test
    void ownerAndAdminBypassCost() {
        RecordingEconomy economy = new RecordingEconomy(false);
        TeleportChargeService service = new TeleportChargeService(new TeleportCostService(economy, PlayerResourceGateway.empty(), false));
        UUID owner = UUID.randomUUID();
        TeleportLocation warp = playerWarp(owner, CostSpec.money(12.5));

        assertThat(service.chargeIfNeeded(owner, false, warp).success()).isTrue();
        assertThat(service.chargeIfNeeded(UUID.randomUUID(), true, warp).success()).isTrue();
        assertThat(economy.withdrawnPlayer).isNull();
    }

    private static TeleportLocation playerWarp(UUID owner, CostSpec cost) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                "player_warp",
                OwnerRef.player(owner),
                "market",
                new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                cost,
                false,
                Instant.EPOCH
        );
    }

    private static final class RecordingEconomy implements EconomyGateway {
        private final boolean withdrawResult;
        private UUID withdrawnPlayer;
        private double withdrawnAmount;

        private RecordingEconomy(boolean withdrawResult) {
            this.withdrawResult = withdrawResult;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public boolean withdraw(UUID playerId, double amount) {
            withdrawnPlayer = playerId;
            withdrawnAmount = amount;
            return withdrawResult;
        }
    }
}
