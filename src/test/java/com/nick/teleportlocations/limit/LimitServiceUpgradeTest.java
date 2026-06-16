package com.nick.teleportlocations.limit;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.VisibilityMode;
import dev.invisiblespiders.haven.api.upgrade.HavenUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LimitServiceUpgradeTest {

    UUID playerId = UUID.randomUUID();
    LimitService limitService;
    HavenUpgradeService upgradeService;

    @BeforeEach
    void setup() {
        upgradeService = mock(HavenUpgradeService.class);
        limitService = new LimitService(categories(), new InMemoryLimitRepository());
    }

    @Test
    void resolveEffectiveLimitIsBaseWhenNoUpgradeService() {
        // upgradeService NOT set → 3 base for homes
        assertThat(limitService.resolveEffectiveLimit(playerId, "home", 2)).isEqualTo(3);
    }

    @Test
    void resolveEffectiveLimitAddsUpgradeBonusSlots() {
        when(upgradeService.currentLevel(playerId, "home-slots")).thenReturn(2);
        limitService.setUpgradeService(upgradeService);
        // base 3 + (2 levels * 2 slots/level) = 7
        assertThat(limitService.resolveEffectiveLimit(playerId, "home", 2)).isEqualTo(7);
    }

    @Test
    void resolveEffectiveLimitLevel0GivesNoBonus() {
        when(upgradeService.currentLevel(playerId, "home-slots")).thenReturn(0);
        limitService.setUpgradeService(upgradeService);
        assertThat(limitService.resolveEffectiveLimit(playerId, "home", 2)).isEqualTo(3);
    }

    @Test
    void resolveEffectiveLimitWorksForWarps() {
        when(upgradeService.currentLevel(playerId, "warp-slots")).thenReturn(3);
        limitService.setUpgradeService(upgradeService);
        // base 2 + (3 * 1) = 5
        assertThat(limitService.resolveEffectiveLimit(playerId, "player_warp", 1)).isEqualTo(5);
    }

    @Test
    void resolveEffectiveLimitWorksForShops() {
        when(upgradeService.currentLevel(playerId, "shop-slots")).thenReturn(1);
        limitService.setUpgradeService(upgradeService);
        // base 1 + (1 * 1) = 2
        assertThat(limitService.resolveEffectiveLimit(playerId, "shop", 1)).isEqualTo(2);
    }

    @Test
    void resolveEffectiveLimitUnknownCategoryGivesNoBonus() {
        when(upgradeService.currentLevel(any(), any())).thenReturn(5);
        limitService.setUpgradeService(upgradeService);
        // "outpost" has no upgrade id → bonus is 0
        assertThat(limitService.resolveEffectiveLimit(playerId, "outpost", 1))
                .isEqualTo(limitService.resolveLimit(playerId, "outpost"));
    }

    private static Map<String, CategoryConfig> categories() {
        return Map.of(
                "home", new CategoryConfig("home", OwnerKind.PLAYER, 3, CreationZone.TRUSTED_CLAIM, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false),
                "player_warp", new CategoryConfig("player_warp", OwnerKind.PLAYER, 2, CreationZone.TRUSTED_CLAIM, AccessMode.PUBLIC, VisibilityMode.LISTED, true, false, false),
                "shop", new CategoryConfig("shop", OwnerKind.PLAYER, 1, CreationZone.TRUSTED_CLAIM, AccessMode.PUBLIC, VisibilityMode.LISTED, false, true, true),
                "outpost", new CategoryConfig("outpost", OwnerKind.PLAYER, 1, CreationZone.WILDERNESS, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false)
        );
    }
}
