package com.nick.teleportlocations.claim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class BukkitHavenClaimsGatewayTest {
    @Test
    void actionKeyUsesCategoryName() {
        assertThat(BukkitHavenClaimsGateway.createActionKey("shop")).isEqualTo("teleportlocations.create.shop");
        assertThat(BukkitHavenClaimsGateway.createActionKey("player_warp")).isEqualTo("teleportlocations.create.player_warp");
    }
}
