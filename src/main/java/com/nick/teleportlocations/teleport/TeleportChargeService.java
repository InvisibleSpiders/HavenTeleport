package com.nick.teleportlocations.teleport;

import com.nick.teleportlocations.cost.ChargeResult;
import com.nick.teleportlocations.cost.TeleportCostService;
import com.nick.teleportlocations.location.OwnerType;
import com.nick.teleportlocations.location.TeleportLocation;
import java.util.UUID;

public final class TeleportChargeService {
    private final TeleportCostService costs;

    public TeleportChargeService(TeleportCostService costs) {
        this.costs = costs;
    }

    public ChargeResult chargeIfNeeded(UUID playerId, boolean adminBypassCost, TeleportLocation location) {
        if (adminBypassCost) {
            return ChargeResult.ok();
        }
        if (location.owner().type() == OwnerType.PLAYER && location.owner().playerIdOptional().filter(playerId::equals).isPresent()) {
            return ChargeResult.ok();
        }
        return costs.charge(playerId, location.category(), location.cost());
    }
}
