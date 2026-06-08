package com.nick.teleportlocations.claim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class BukkitLandClaimsGatewayTest {
    @Test
    void actionKeyUsesCategoryName() {
        assertThat(BukkitLandClaimsGateway.createActionKey("shop")).isEqualTo("teleportlocations.create.shop");
        assertThat(BukkitLandClaimsGateway.createActionKey("player_warp")).isEqualTo("teleportlocations.create.player_warp");
    }
}
