package com.nick.teleportlocations.shop;

import dev.invisiblespiders.haven.api.service.HavenWarpService;
import java.util.List;
import java.util.UUID;

public final class HavenTeleportWarpService implements HavenWarpService {
    private final ShopWarpService shops;

    public HavenTeleportWarpService(ShopWarpService shops) {
        this.shops = shops;
    }

    @Override
    public List<String> getShopWarps(UUID playerUuid) {
        return shops.ownerShops(playerUuid).stream()
                .map(location -> location.name())
                .toList();
    }

    @Override
    public boolean hasShopWarp(UUID playerUuid, String warpName) {
        return shops.ownerShops(playerUuid).stream()
                .anyMatch(location -> location.normalizedName().equalsIgnoreCase(warpName));
    }
}
