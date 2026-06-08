package com.nick.teleportlocations.cost;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.CostSpec;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportCostServiceTest {
    @Test
    void shopCostIsRejectedBeforeCharging() {
        TeleportCostService service = new TeleportCostService(EconomyGateway.unavailable(), PlayerResourceGateway.empty(), false);

        ChargeResult result = service.charge(UUID.randomUUID(), "shop", CostSpec.money(10.0));

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("shop-free-only");
    }

    @Test
    void moneyCostIsDeniedWhenEconomyMissing() {
        TeleportCostService service = new TeleportCostService(EconomyGateway.unavailable(), PlayerResourceGateway.empty(), false);

        ChargeResult result = service.charge(UUID.randomUUID(), "player_warp", CostSpec.money(10.0));

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("economy-missing");
    }

    @Test
    void moneyCostCanBeFreeWhenConfiguredForOutage() {
        TeleportCostService service = new TeleportCostService(EconomyGateway.unavailable(), PlayerResourceGateway.empty(), true);

        ChargeResult result = service.charge(UUID.randomUUID(), "player_warp", CostSpec.money(10.0));

        assertThat(result.success()).isTrue();
    }
}
