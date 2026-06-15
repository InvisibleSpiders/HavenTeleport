package com.nick.teleportlocations.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.claim.CreationPolicyService;
import com.nick.teleportlocations.claim.HavenClaimsGateway;
import com.nick.teleportlocations.claim.MissingHavenClaimsPolicy;
import com.nick.teleportlocations.config.ConfigLoader;
import com.nick.teleportlocations.limit.InMemoryLimitRepository;
import com.nick.teleportlocations.limit.LimitService;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.storage.InMemoryLocationRepository;
import dev.invisiblespiders.haven.api.service.HavenWarpService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HavenTeleportWarpServiceTest {
    @Test
    void exposesOwnedShopWarpsToHavenCoreMarketService() {
        UUID owner = UUID.randomUUID();
        ShopWarpService shops = createShopService();
        shops.setShop(owner, "Tools", position(), false);
        HavenWarpService service = new HavenTeleportWarpService(shops);

        assertThat(service.getShopWarps(owner)).containsExactly("Tools");
        assertThat(service.hasShopWarp(owner, "tools")).isTrue();
        assertThat(service.hasShopWarp(owner, "food")).isFalse();
    }

    private static ShopWarpService createShopService() {
        var config = ConfigLoader.fromResources();
        var repository = new InMemoryLocationRepository();
        var locations = new LocationService(repository, () -> Instant.EPOCH);
        var limits = new LimitService(config.categories(), new InMemoryLimitRepository());
        var creationPolicy = new CreationPolicyService(
                config.categories(),
                HavenClaimsGateway.fixedOwned(true, true, true),
                MissingHavenClaimsPolicy.DENY_CLAIM_REQUIRED
        );
        return new ShopWarpService(locations, limits, creationPolicy);
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 1.0, 64.0, 2.0, 90.0f, 0.0f);
    }
}
