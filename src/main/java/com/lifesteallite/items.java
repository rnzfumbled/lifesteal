package com.lifesteallite;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Creates and identifies the two custom items. No caching, no extra state —
 * items are built on demand, which keeps memory footprint minimal.
 */
public final class Items {

    public final NamespacedKey heartKey;
    public final NamespacedKey reviveTotemKey;

    public Items(LifestealLite plugin) {
        this.heartKey = new NamespacedKey(plugin, "heart");
        this.reviveTotemKey = new NamespacedKey(plugin, "revive_totem");
    }

    public ItemStack createHeart(int amount) {
        ItemStack item = new ItemStack(Material.REDSTONE, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Heart");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Right-click to consume and gain a heart.",
                ChatColor.GRAY + "Also used to craft a Revive Totem."
        ));
        meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHeart(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(heartKey, PersistentDataType.BYTE);
    }

    public ItemStack createReviveTotem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Revive Totem");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Right-click an eliminated player",
                ChatColor.GRAY + "to bring them back."
        ));
        meta.getPersistentDataContainer().set(reviveTotemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isReviveTotem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(reviveTotemKey, PersistentDataType.BYTE);
    }
}
