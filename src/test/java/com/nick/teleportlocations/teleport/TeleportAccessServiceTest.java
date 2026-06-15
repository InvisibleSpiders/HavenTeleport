package com.nick.teleportlocations.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportAccessServiceTest {
    @Test
    void allowsUnclaimedDestinations() {
        TeleportAccessService service = new TeleportAccessService(HavenClaimsGateway.fixed(false, false));

        TeleportAccessResult result = service.canEnter(UUID.randomUUID(), position(), false);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void deniesClaimedDestinationsWithoutEntryAccess() {
        TeleportAccessService service = new TeleportAccessService(HavenClaimsGateway.fixed(true, false));

        TeleportAccessResult result = service.canEnter(UUID.randomUUID(), position(), false);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("claim-entry-denied");
    }

    @Test
    void allowsClaimedDestinationsWithEntryAccess() {
        TeleportAccessService service = new TeleportAccessService(HavenClaimsGateway.fixed(true, true));

        TeleportAccessResult result = service.canEnter(UUID.randomUUID(), position(), false);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void adminBypassAllowsClaimedDestinations() {
        TeleportAccessService service = new TeleportAccessService(HavenClaimsGateway.fixed(true, false));

        TeleportAccessResult result = service.canEnter(UUID.randomUUID(), position(), true);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void missingHavenClaimsDoesNotBlockTeleports() {
        TeleportAccessService service = new TeleportAccessService(HavenClaimsGateway.missing());

        TeleportAccessResult result = service.canEnter(UUID.randomUUID(), position(), false);

        assertThat(result.allowed()).isTrue();
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 0.0f, 0.0f);
    }
}
