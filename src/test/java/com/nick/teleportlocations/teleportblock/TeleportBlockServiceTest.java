package com.nick.teleportlocations.teleportblock;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportBlockServiceTest {
    private static final UUID WORLD_ID = UUID.randomUUID();

    @Test
    void placesOnlyInOwnClaimUnlessAdminBypass() {
        UUID owner = UUID.randomUUID();
        TeleportBlockService denied = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixed(true, true), () -> Instant.EPOCH);

        TeleportBlockResult deniedResult = denied.place(owner, position(0, 64, 0), false);

        assertThat(deniedResult.status()).isEqualTo(TeleportBlockResult.Status.CLAIM_DENIED);

        TeleportBlockService allowed = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        TeleportBlockResult allowedResult = allowed.place(owner, position(0, 64, 0), false);

        assertThat(allowedResult.status()).isEqualTo(TeleportBlockResult.Status.PLACED);
        assertThat(allowedResult.block()).isPresent();
    }

    @Test
    void buildAccessCanBreakTeleportBlock() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        TeleportBlockService service = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        service.place(owner, position(0, 64, 0), false);

        TeleportBlockResult result = service.breakBlock(trusted, position(0, 64, 0), false);

        assertThat(result.status()).isEqualTo(TeleportBlockResult.Status.REMOVED);
    }

    @Test
    void linksTwoEditableBlocksWithinDistance() {
        UUID owner = UUID.randomUUID();
        TeleportBlockService service = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        TeleportBlock first = service.place(owner, position(0, 64, 0), false).block().orElseThrow();
        TeleportBlock second = service.place(owner, position(8, 64, 0), false).block().orElseThrow();

        TeleportBlockResult result = service.link(owner, first.position(), second.position(), false, 64);

        assertThat(result.status()).isEqualTo(TeleportBlockResult.Status.LINKED);
        assertThat(service.findAt(first.position()).orElseThrow().linkedBlockId()).contains(second.id());
        assertThat(service.findAt(second.position()).orElseThrow().linkedBlockId()).contains(first.id());
    }

    @Test
    void rejectsLinksBeyondConfiguredDistance() {
        UUID owner = UUID.randomUUID();
        TeleportBlockService service = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        TeleportBlock first = service.place(owner, position(0, 64, 0), false).block().orElseThrow();
        TeleportBlock second = service.place(owner, position(65, 64, 0), false).block().orElseThrow();

        TeleportBlockResult result = service.link(owner, first.position(), second.position(), false, 64);

        assertThat(result.status()).isEqualTo(TeleportBlockResult.Status.DISTANCE_TOO_FAR);
        assertThat(service.findAt(first.position()).orElseThrow().linkedBlockId()).isEmpty();
        assertThat(service.findAt(second.position()).orElseThrow().linkedBlockId()).isEmpty();
    }

    @Test
    void linkedDestinationResolvesOtherBlock() {
        UUID owner = UUID.randomUUID();
        TeleportBlockService service = new TeleportBlockService(new InMemoryTeleportBlockRepository(), LandClaimsGateway.fixedOwned(true, true, true), () -> Instant.EPOCH);
        TeleportBlock first = service.place(owner, position(0, 64, 0), false).block().orElseThrow();
        TeleportBlock second = service.place(owner, position(4, 64, 0), false).block().orElseThrow();
        service.link(owner, first.position(), second.position(), false, 64);

        assertThat(service.linkedDestination(first)).map(TeleportBlock::id).contains(second.id());
    }

    private static SavedPosition position(int x, int y, int z) {
        return new SavedPosition(WORLD_ID, "world", x, y, z, 0.0f, 0.0f);
    }
}
