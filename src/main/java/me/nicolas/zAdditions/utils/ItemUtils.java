package me.nicolas.zAdditions.utils;

import me.nicolas.zAdditions.ZAdditions;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {

    public static void setCustomTag(ItemMeta meta, String tag) {
        NamespacedKey key = new NamespacedKey(ZAdditions.getInstance(), tag);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tag);
    }

    public static boolean hasCustomTag(ItemMeta meta, String tag) {
        NamespacedKey key = new NamespacedKey(ZAdditions.getInstance(), tag);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static String getCustomTag(ItemMeta meta, String tag) {
        NamespacedKey key = new NamespacedKey(ZAdditions.getInstance(), tag);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}