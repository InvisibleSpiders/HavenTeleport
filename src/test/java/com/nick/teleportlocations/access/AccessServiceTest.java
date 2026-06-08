package com.nick.teleportlocations.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.CostSpec;
import com.nick.teleportlocations.location.OwnerRef;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.VisibilityMode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AccessServiceTest {
    @Test
    void ownerCanUsePrivateLocation() {
        UUID owner = UUID.randomUUID();
        AccessService service = new AccessService();

        assertThat(service.canUse(location(owner, AccessMode.PRIVATE), new AccessContext(owner, false, false)).allowed()).isTrue();
    }

    @Test
    void publicLocationAllowsNonOwner() {
        AccessService service = new AccessService();

        assertThat(service.canUse(location(UUID.randomUUID(), AccessMode.PUBLIC), new AccessContext(UUID.randomUUID(), false, false)).allowed()).isTrue();
    }

    @Test
    void trustedLocationRequiresTrustForNonOwner() {
        AccessService service = new AccessService();

        assertThat(service.canUse(location(UUID.randomUUID(), AccessMode.TRUSTED), new AccessContext(UUID.randomUUID(), false, false)).allowed()).isFalse();
        assertThat(service.canUse(location(UUID.randomUUID(), AccessMode.TRUSTED), new AccessContext(UUID.randomUUID(), false, true)).allowed()).isTrue();
    }

    private static TeleportLocation location(UUID owner, AccessMode accessMode) {
        return TeleportLocation.create(
                UUID.randomUUID(),
                "player_warp",
                OwnerRef.player(owner),
                "base",
                new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 1.0, 0.0f, 0.0f),
                accessMode,
                VisibilityMode.LISTED,
                CostSpec.free(),
                false,
                Instant.EPOCH
        );
    }
}
