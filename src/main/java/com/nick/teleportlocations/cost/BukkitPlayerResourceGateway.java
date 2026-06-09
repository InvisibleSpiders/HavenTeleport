package com.nick.teleportlocations.cost;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BukkitPlayerResourceGateway implements PlayerResourceGateway {
    @Override
    public boolean takeXpLevels(UUID playerId, int levels) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.getLevel() < levels) {
            return false;
        }
        int currentLevel = player.getLevel();
        if (currentLevel < levels) {
            return false;
        }
        player.setLevel(currentLevel - levels);
        return true;
    }

    @Override
    public boolean takeXpPoints(UUID playerId, int points) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.getTotalExperience() < points) {
            return false;
        }
        int currentPoints = player.getTotalExperience();
        if (currentPoints < points) {
            return false;
        }
        player.setTotalExperience(currentPoints - points);
        return true;
    }

    @Override
    public boolean takeItem(UUID playerId, String materialName, int amount) {
        Player player = Bukkit.getPlayer(playerId);
        Material material = Material.matchMaterial(materialName);
        if (player == null || material == null || amount <= 0) {
            return false;
        }
        ItemStack stack = new ItemStack(material, amount);
        if (!player.getInventory().containsAtLeast(stack, amount)) {
            return false;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().removeItem(stack);
        return leftovers.isEmpty();
    }
}
