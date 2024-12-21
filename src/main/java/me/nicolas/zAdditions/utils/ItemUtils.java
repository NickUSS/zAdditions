package me.nicolas.zAdditions.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemUtils {
    public static ItemStack createCustomItem(ItemStack item, String displayName, List<String> lore, String tag) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);

        NamespacedKey key = new NamespacedKey("zadditions", tag);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tag);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean hasCustomTag(ItemStack item, String tag) {
        if (item == null || !item.hasItemMeta()) return false;

        NamespacedKey key = new NamespacedKey("zadditions", tag);
        return item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.STRING);
    }
}
