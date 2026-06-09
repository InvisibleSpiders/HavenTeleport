package com.nick.teleportlocations.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AdminBypassServiceTest {
    @Test
    void togglesClaimBypassPerPlayer() {
        AdminBypassService service = new AdminBypassService();
        UUID playerId = UUID.randomUUID();

        assertThat(service.claims(playerId)).isFalse();
        assertThat(service.toggleClaims(playerId)).isTrue();
        assertThat(service.claims(playerId)).isTrue();
        assertThat(service.toggleClaims(playerId)).isFalse();
        assertThat(service.claims(playerId)).isFalse();
    }

    @Test
    void canSetClaimBypassExplicitly() {
        AdminBypassService service = new AdminBypassService();
        UUID playerId = UUID.randomUUID();

        service.setClaims(playerId, true);
        assertThat(service.claims(playerId)).isTrue();

        service.setClaims(playerId, false);
        assertThat(service.claims(playerId)).isFalse();
    }
}
