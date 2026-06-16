package com.nick.teleportlocations.limit;

import com.nick.teleportlocations.category.CategoryConfig;
import dev.invisiblespiders.haven.api.upgrade.HavenUpgradeService;
import org.jetbrains.annotations.Nullable;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public final class LimitService {
    private final Map<String, CategoryConfig> categories;
    private final LimitRepository repository;
    private volatile @Nullable HavenUpgradeService upgradeService;

    public LimitService(Map<String, CategoryConfig> categories, LimitRepository repository) {
        this.categories = Map.copyOf(categories);
        this.repository = repository;
    }

    public void setUpgradeService(@Nullable HavenUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }

    public int resolveLimit(UUID playerId, String category) {
        CategoryConfig config = categories.get(category);
        if (config == null) {
            throw new IllegalArgumentException("unknown category: " + category);
        }
        return repository.findLimit(playerId, category)
                .orElseGet(config::defaultLimit);
    }

    public boolean hasCategory(String category) {
        return categories.containsKey(category);
    }

    public Set<String> categoryKeys() {
        return categories.keySet();
    }

    public void setLimit(UUID playerId, String category, int amount) {
        repository.setLimit(playerId, category, Math.max(0, amount));
    }

    public void addLimit(UUID playerId, String category, int amount) {
        setLimit(playerId, category, resolveLimit(playerId, category) + Math.max(0, amount));
    }

    public void removeLimit(UUID playerId, String category, int amount) {
        setLimit(playerId, category, Math.max(0, resolveLimit(playerId, category) - Math.max(0, amount)));
    }

    public void clearLimit(UUID playerId, String category) {
        repository.clearLimit(playerId, category);
    }

    public int resolveUpgradeBonus(UUID playerId, String category) {
        if (upgradeService == null) return 0;
        String upgradeId = upgradeIdForCategory(category);
        if (upgradeId == null) return 0;
        return Math.max(0, upgradeService.currentLevel(playerId, upgradeId));
    }

    public int resolveEffectiveLimit(UUID playerId, String category, int slotsPerLevel) {
        int base = resolveLimit(playerId, category);
        int level = resolveUpgradeBonus(playerId, category);
        return base + (level * slotsPerLevel);
    }

    private static @Nullable String upgradeIdForCategory(String category) {
        return switch (category) {
            case "home" -> "home-slots";
            case "player_warp" -> "warp-slots";
            case "shop" -> "shop-slots";
            default -> null;
        };
    }
}
