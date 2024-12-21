package me.nicolas.zAdditions.listeners;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class ChunkMinerListener implements Listener {
    private final ChunkMinerManager minerManager;

    public ChunkMinerListener(ChunkMinerManager minerManager) {
        this.minerManager = minerManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ChunkMinerItem.isChunkMiner(event.getItemInHand())) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("zadditions.chunkminer.place")) {
            event.setCancelled(true);
            player.sendMessage("§cNo tienes permiso para colocar Chunk Miners.");
            return;
        }

        if (minerManager.hasMinerInChunk(event.getBlock().getChunk())) {
            event.setCancelled(true);
            player.sendMessage("§cYa hay un Chunk Miner en este chunk.");
            return;
        }

        if (!minerManager.canPlaceMiner(event.getBlock().getWorld())) {
            event.setCancelled(true);
            player.sendMessage("§cHas alcanzado el límite de Chunk Miners en este mundo (10).");
            return;
        }

        minerManager.addMiner(event.getBlock().getLocation(), player.getUniqueId());
        player.sendMessage("§aChunk Miner colocado correctamente.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (minerManager.getMiner(event.getBlock().getLocation()) != null) {
            minerManager.removeMiner(event.getBlock().getLocation());
            event.getPlayer().sendMessage("§aChunk Miner removido correctamente.");
        }
    }
}
