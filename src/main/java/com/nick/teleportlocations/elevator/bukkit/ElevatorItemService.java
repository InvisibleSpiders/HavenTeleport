package com.nick.teleportlocations.elevator.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class ElevatorItemService {
    private static final byte MARKER_VALUE = 1;

    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    public ElevatorItemService(Plugin plugin) {
        String namespace = plugin.getName().toLowerCase(Locale.ROOT);
        this.itemKey = new NamespacedKey(namespace, "elevator_block");
        this.recipeKey = new NamespacedKey(namespace, "elevator_block_recipe");
    }

    public NamespacedKey recipeKey() {
        return recipeKey;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Elevator Block", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Jump to go up.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak to go down.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, MARKER_VALUE);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isElevatorItem(ItemStack item) {
        if (item == null || item.getType() != Material.LODESTONE || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.BYTE);
        return marker != null && marker == MARKER_VALUE;
    }

    public ShapedRecipe createRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createItem());
        recipe.shape(" E ", "CLC", " E ");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('L', Material.LODESTONE);
        return recipe;
    }
}
