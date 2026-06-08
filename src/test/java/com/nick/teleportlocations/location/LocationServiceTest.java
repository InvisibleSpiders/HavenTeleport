package com.nick.teleportlocations.location;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LocationServiceTest {
    @Test
    void settingNewMainHomeUnsetsOldMainHome() {
        UUID playerId = UUID.randomUUID();
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        LocationService service = new LocationService(repository, () -> Instant.EPOCH);

        TeleportLocation first = service.createOrUpdate(request(playerId, "home", "base", true));
        TeleportLocation second = service.createOrUpdate(request(playerId, "home", "mine", true));

        assertThat(repository.findById(first.id()).orElseThrow().mainHome()).isFalse();
        assertThat(repository.findById(second.id()).orElseThrow().mainHome()).isTrue();
        assertThat(service.mainHome(playerId)).contains(repository.findById(second.id()).orElseThrow());
    }

    @Test
    void createOrUpdateUsesOwnerCategoryAndNormalizedNameAsIdentity() {
        UUID playerId = UUID.randomUUID();
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        LocationService service = new LocationService(repository, () -> Instant.EPOCH);

        service.createOrUpdate(request(playerId, "home", "Main Base", false));
        service.createOrUpdate(request(playerId, "home", "main   base", true));

        assertThat(repository.findByOwnerAndCategory(OwnerRef.player(playerId), "home")).hasSize(1);
        assertThat(service.mainHome(playerId)).isPresent();
    }

    private static CreateLocationRequest request(UUID playerId, String category, String name, boolean mainHome) {
        return new CreateLocationRequest(
                category,
                OwnerRef.player(playerId),
                name,
                new SavedPosition(UUID.randomUUID(), "world", 1.0, 65.0, 2.0, 0.0f, 0.0f),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.free(),
                mainHome
        );
    }
}
