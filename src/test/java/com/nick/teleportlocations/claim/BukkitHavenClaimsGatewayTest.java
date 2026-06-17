package com.nick.teleportlocations.claim;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.SavedPosition;
import org.junit.jupiter.api.Test;

final class BukkitHavenClaimsGatewayTest {
    @Test
    void actionKeyUsesCategoryName() {
        assertThat(BukkitHavenClaimsGateway.createActionKey("shop")).isEqualTo("teleportlocations.create.shop");
        assertThat(BukkitHavenClaimsGateway.createActionKey("player_warp")).isEqualTo("teleportlocations.create.player_warp");
    }

    @Test
    void missingGatewayDoesNotAllowVisitorEntry() {
        assertThat(HavenClaimsGateway.missing().canVisitorsEnter(position())).isFalse();
    }

    @Test
    void fixedOwnedGatewayCanModelVisitorEntry() {
        assertThat(HavenClaimsGateway.fixedOwned(true, true, true).canVisitorsEnter(position())).isTrue();
        assertThat(HavenClaimsGateway.fixedOwned(true, false, true).canVisitorsEnter(position())).isFalse();
    }

    private static SavedPosition position() {
        return new SavedPosition(java.util.UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f);
    }
}
