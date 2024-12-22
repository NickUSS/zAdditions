package me.nicolas.zAdditions.listeners;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import me.nicolas.zAdditions.miners.ChunkMiner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.Action;

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

        if (minerManager.hasReachedGlobalLimit()) {
            event.setCancelled(true);
            player.sendMessage("§cSe ha alcanzado el límite global de Chunk Miners.");
            return;
        }

        minerManager.addMiner(event.getBlock().getLocation(), player.getUniqueId());
        player.sendMessage("§aChunk Miner colocado correctamente.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ChunkMiner miner = minerManager.getMiner(event.getBlock().getLocation());
        if (miner != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            // Verificar si es el dueño o tiene permisos de admin
            if (!player.getUniqueId().equals(miner.getOwnerUUID()) &&
                    !player.hasPermission("zadditions.chunkminer.admin")) {
                player.sendMessage("§cSolo el dueño o un administrador puede romper este Chunk Miner.");
                return;
            }

            // Dar el item al jugador
            player.getInventory().addItem(ChunkMinerItem.create());

            // Remover el miner
            miner.remove(false); // false porque ya dimos el item manualmente
            minerManager.removeMiner(event.getBlock().getLocation());
            player.sendMessage("§aChunk Miner removido y devuelto a tu inventario.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEACON) return;

        ChunkMiner miner = minerManager.getMiner(event.getClickedBlock().getLocation());
        if (miner != null) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            if (!player.getUniqueId().equals(miner.getOwnerUUID()) &&
                    !player.hasPermission("zadditions.chunkminer.access.others")) {
                player.sendMessage("§cNo puedes acceder al inventario del Chunk Miner de otro jugador.");
                return;
            }

            miner.openStorage(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Chunk Miner Storage")) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                // Permitir sacar items pero no poner
                if (event.isShiftClick() || event.getClick().isKeyboardClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals("§6Chunk Miner Storage")) {
            event.setCancelled(true);
        }
    }
}