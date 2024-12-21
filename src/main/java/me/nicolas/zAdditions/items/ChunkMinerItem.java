package me.nicolas.zAdditions.items;

import me.nicolas.zAdditions.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ChunkMinerItem {
    private static final String DISPLAY_NAME = "§6Chunk Miner";
    private static final String[] LORE = {
            "§7Coloca este item para minar",
            "§7automáticamente un chunk completo",
            "",
            "§c⚠ Solo funciona si el dueño está online",
            "§c⚠ Límite: 10 por mundo"
    };

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BEACON);
        return ItemUtils.createCustomItem(item, DISPLAY_NAME, Arrays.asList(LORE), "chunk_miner");
    }

    public static boolean isChunkMiner(ItemStack item) {
        return ItemUtils.hasCustomTag(item, "chunk_miner");
    }
}
