package com.nick.teleportlocations.serverwarp;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ServerWarpServiceTest {
    @Test
    void createsListedPublicServerWarp() {
        ServerWarpService service = new ServerWarpService(new LocationService(new InMemoryLocationRepository(), () -> Instant.EPOCH));

        ServerWarpResult result = service.setWarp("spawn-market", position());

        assertThat(result.status()).isEqualTo(ServerWarpResult.Status.CREATED);
        assertThat(service.visibleWarps()).extracting("name").containsExactly("spawn-market");
        assertThat(service.resolveVisibleWarp("spawn-market")).isPresent();
    }

    @Test
    void deletesServerWarpByName() {
        ServerWarpService service = new ServerWarpService(new LocationService(new InMemoryLocationRepository(), () -> Instant.EPOCH));
        service.setWarp("spawn-market", position());

        ServerWarpResult result = service.deleteWarp("spawn-market");

        assertThat(result.status()).isEqualTo(ServerWarpResult.Status.DELETED);
        assertThat(service.visibleWarps()).isEmpty();
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }
}
