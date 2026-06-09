package com.nick.teleportlocations.limit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LimitServiceTest {
    @Test
    void usesDefaultLimitWhenNoOverrideExists() {
        LimitService service = new LimitService(categories(), new InMemoryLimitRepository());

        assertThat(service.resolveLimit(UUID.randomUUID(), "home")).isEqualTo(3);
    }

    @Test
    void adminOverrideReplacesDefault() {
        InMemoryLimitRepository repository = new InMemoryLimitRepository();
        UUID playerId = UUID.randomUUID();
        LimitService service = new LimitService(categories(), repository);

        service.setLimit(playerId, "home", 7);

        assertThat(service.resolveLimit(playerId, "home")).isEqualTo(7);
    }

    @Test
    void addAndRemoveLimitAdjustOnlyOneCategory() {
        UUID playerId = UUID.randomUUID();
        LimitService service = new LimitService(categories(), new InMemoryLimitRepository());

        service.addLimit(playerId, "shop", 2);
        service.removeLimit(playerId, "shop", 1);

        assertThat(service.resolveLimit(playerId, "shop")).isEqualTo(2);
        assertThat(service.resolveLimit(playerId, "home")).isEqualTo(3);
    }

    @Test
    void unknownCategoryFailsWithDescriptiveError() {
        LimitService service = new LimitService(categories(), new InMemoryLimitRepository());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.resolveLimit(UUID.randomUUID(), "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown category: missing");
    }

    private static Map<String, CategoryConfig> categories() {
        return Map.of(
                "home", new CategoryConfig("home", OwnerKind.PLAYER, 3, CreationZone.TRUSTED_CLAIM, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false),
                "shop", new CategoryConfig("shop", OwnerKind.PLAYER, 1, CreationZone.TRUSTED_CLAIM, AccessMode.PUBLIC, VisibilityMode.LISTED, false, true, true)
        );
    }
}
