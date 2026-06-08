package com.nick.teleportlocations.elevator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ElevatorServiceTest {
    private static final UUID WORLD_ID = UUID.randomUUID();

    @Test
    void placesElevatorOnlyInOwnClaimUnlessAdminBypass() {
        UUID owner = UUID.randomUUID();
        ElevatorService denied = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixed(true, true), () -> Instant.EPOCH);

        ElevatorResult deniedResult = denied.place(owner, position(64), false);

        assertThat(deniedResult.status()).isEqualTo(ElevatorResult.Status.CLAIM_DENIED);

        ElevatorService allowed = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        ElevatorResult allowedResult = allowed.place(owner, position(64), false);

        assertThat(allowedResult.status()).isEqualTo(ElevatorResult.Status.PLACED);
        assertThat(allowedResult.block()).isPresent();
    }

    @Test
    void buildAccessCanBreakElevator() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        ElevatorService service = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        service.place(owner, position(64), false);

        ElevatorResult result = service.breakBlock(trusted, position(64), false);

        assertThat(result.status()).isEqualTo(ElevatorResult.Status.REMOVED);
    }

    @Test
    void claimAccessCanUseElevator() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        ElevatorService service = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        service.place(owner, position(64), false);

        assertThat(service.canUse(trusted, position(64), false)).isTrue();
        ElevatorService denied = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, false, true), () -> Instant.EPOCH);
        denied.place(owner, position(64), false);
        assertThat(denied.canUse(trusted, position(64), false)).isFalse();
    }

    @Test
    void findsNearestFloorAboveAndBelowWithinMaxDistance() {
        UUID owner = UUID.randomUUID();
        ElevatorService service = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        service.place(owner, position(64), false);
        service.place(owner, position(70), false);
        service.place(owner, position(82), false);

        assertThat(service.findDestination(position(64), ElevatorDirection.UP, 16)).map(ElevatorBlock::blockY).contains(70);
        assertThat(service.findDestination(position(82), ElevatorDirection.DOWN, 16)).map(ElevatorBlock::blockY).contains(70);
        assertThat(service.findDestination(position(64), ElevatorDirection.UP, 4)).isEmpty();
    }

    @Test
    void storesParticleSelection() {
        UUID owner = UUID.randomUUID();
        ElevatorService service = new ElevatorService(new InMemoryElevatorRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        service.place(owner, position(64), false);

        ElevatorResult result = service.setParticle(owner, position(64), ElevatorParticle.END_ROD, false);

        assertThat(result.status()).isEqualTo(ElevatorResult.Status.UPDATED);
        assertThat(service.findAt(position(64)).orElseThrow().particle()).isEqualTo(ElevatorParticle.END_ROD);
    }

    private static SavedPosition position(int y) {
        return new SavedPosition(WORLD_ID, "world", 12.0, y, 22.0, 0.0f, 0.0f);
    }
}
