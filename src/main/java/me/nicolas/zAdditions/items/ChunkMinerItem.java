package me.nicolas.zAdditions.items;

import me.nicolas.zAdditions.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ChunkMinerItem {
    private static final String DISPLAY_NAME = "§6§lChunk Miner";
    private static final List<String> LORE = Arrays.asList(
            "§7Coloca este item para minar",
            "§7automáticamente un chunk completo",
            "",
            "§e► Click derecho para ver el inventario",
            "§e► Rompe el beacon para recuperarlo",
            "",
            "§8• §7Velocidad: §e1 bloque/1s",
            "§8• §7Radio: §eChunk completo",
            "",
            "§c⚠ Solo funciona si el dueño está online",
            "§c⚠ Límite: 10 por mundo"
    );

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(DISPLAY_NAME);
        meta.setLore(LORE);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ItemUtils.setCustomTag(meta, "chunk_miner");

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isChunkMiner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return ItemUtils.hasCustomTag(item.getItemMeta(), "chunk_miner");
    }
}