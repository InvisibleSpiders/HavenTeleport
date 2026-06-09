package com.nick.teleportlocations.elevator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class ElevatorActivationServiceTest {
    private static final UUID WORLD_ID = UUID.randomUUID();

    @Test
    void resolvesDestinationAndConsumesCooldown() {
        UUID playerId = UUID.randomUUID();
        ElevatorService elevators = elevatorService(true);
        elevators.place(playerId, position(64), false);
        elevators.place(playerId, position(70), false);
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        ElevatorActivationService activations = new ElevatorActivationService(
                elevators,
                new ElevatorCooldownService(2, now::get),
                16
        );

        ElevatorActivationResult result = activations.activate(playerId, position(64), ElevatorDirection.UP, false, false);

        assertThat(result.status()).isEqualTo(ElevatorActivationResult.Status.TELEPORT);
        assertThat(result.destination()).map(ElevatorBlock::blockY).contains(70);
        ElevatorActivationResult cooldown = activations.activate(playerId, position(64), ElevatorDirection.UP, false, false);
        assertThat(cooldown.status()).isEqualTo(ElevatorActivationResult.Status.COOLDOWN);
        assertThat(cooldown.remainingCooldownSeconds()).isEqualTo(2);
    }

    @Test
    void reportsMissingDestinationBeforeCooldown() {
        UUID playerId = UUID.randomUUID();
        ElevatorService elevators = elevatorService(true);
        elevators.place(playerId, position(64), false);
        ElevatorActivationService activations = new ElevatorActivationService(
                elevators,
                new ElevatorCooldownService(2, () -> Instant.EPOCH),
                16
        );

        ElevatorActivationResult result = activations.activate(playerId, position(64), ElevatorDirection.UP, false, false);

        assertThat(result.status()).isEqualTo(ElevatorActivationResult.Status.NO_DESTINATION);
        assertThat(activations.activate(playerId, position(64), ElevatorDirection.UP, false, false).status())
                .isEqualTo(ElevatorActivationResult.Status.NO_DESTINATION);
    }

    @Test
    void deniesUseWithoutClaimAccess() {
        UUID ownerId = UUID.randomUUID();
        UUID visitorId = UUID.randomUUID();
        ElevatorService elevators = elevatorService(false);
        elevators.place(ownerId, position(64), false);
        elevators.place(ownerId, position(70), false);
        ElevatorActivationService activations = new ElevatorActivationService(
                elevators,
                new ElevatorCooldownService(2, () -> Instant.EPOCH),
                16
        );

        ElevatorActivationResult result = activations.activate(visitorId, position(64), ElevatorDirection.UP, false, false);

        assertThat(result.status()).isEqualTo(ElevatorActivationResult.Status.ACCESS_DENIED);
    }

    private static ElevatorService elevatorService(boolean canInteract) {
        return new ElevatorService(
                new InMemoryElevatorRepository(),
                LandClaimsGateway.fixedOwned(true, canInteract, true),
                () -> Instant.EPOCH
        );
    }

    private static SavedPosition position(int y) {
        return new SavedPosition(WORLD_ID, "world", 12.0, y, 22.0, 0.0f, 0.0f);
    }
}
