package com.nick.teleportlocations.elevator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class ElevatorCooldownServiceTest {
    @Test
    void allowsFirstUseAndBlocksUntilCooldownExpires() {
        UUID playerId = UUID.randomUUID();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        ElevatorCooldownService cooldowns = new ElevatorCooldownService(2, now::get);

        assertThat(cooldowns.tryUse(playerId, false)).isTrue();
        assertThat(cooldowns.tryUse(playerId, false)).isFalse();
        assertThat(cooldowns.remainingSeconds(playerId)).isEqualTo(2);

        now.set(Instant.EPOCH.plusSeconds(2));

        assertThat(cooldowns.tryUse(playerId, false)).isTrue();
    }

    @Test
    void bypassDoesNotConsumeCooldown() {
        UUID playerId = UUID.randomUUID();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        ElevatorCooldownService cooldowns = new ElevatorCooldownService(2, now::get);

        assertThat(cooldowns.tryUse(playerId, true)).isTrue();
        assertThat(cooldowns.tryUse(playerId, false)).isTrue();
    }

    @Test
    void clearRemovesStoredCooldown() {
        UUID playerId = UUID.randomUUID();
        ElevatorCooldownService cooldowns = new ElevatorCooldownService(2, () -> Instant.EPOCH);

        cooldowns.tryUse(playerId, false);
        cooldowns.clear(playerId);

        assertThat(cooldowns.remainingSeconds(playerId)).isZero();
        assertThat(cooldowns.tryUse(playerId, false)).isTrue();
    }
}
