package me.nicolas.zAdditions.listeners;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import me.nicolas.zAdditions.miners.ChunkMiner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChunkMinerListener implements Listener {
    private final ChunkMinerManager minerManager;

    public ChunkMinerListener(ChunkMinerManager minerManager) {
        if (minerManager == null) {
            throw new IllegalArgumentException("ChunkMinerManager no puede ser null");
        }
        this.minerManager = minerManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§6Chunk Miner Storage")) {
            return;
        }

        // Cancelar cualquier click en la última fila (slots 45-53)
        if (event.getRawSlot() >= 45 && event.getRawSlot() <= 53) {
            event.setCancelled(true);
            return;
        }

        // Prevenir shift-click de items hacia la última fila
        if (event.getClick().isShiftClick() && event.getRawSlot() < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                Inventory clicked_inventory = event.getClickedInventory();
                if (clicked_inventory != null && clicked_inventory.equals(event.getView().getBottomInventory())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        // Manejar navegación entre páginas
        if (clicked != null && clicked.getType() == Material.ARROW) {
            event.setCancelled(true);

            String title = event.getView().getTitle();
            int currentPage = Integer.parseInt(title.split("Página ")[1].replace(")", "")) - 1;

            ChunkMiner miner = null;
            for (ChunkMiner m : minerManager.getAllMiners()) {
                if (m.getStoragePages().contains(event.getInventory())) {
                    miner = m;
                    break;
                }
            }

            if (miner != null) {
                if (clicked.getItemMeta().getDisplayName().equals("§ePágina Anterior") && currentPage > 0) {
                    player.openInventory(miner.getStoragePage(currentPage - 1));
                } else if (clicked.getItemMeta().getDisplayName().equals("§ePágina Siguiente") &&
                        currentPage < miner.getStoragePages().size() - 1) {
                    player.openInventory(miner.getStoragePage(currentPage + 1));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BEACON) return;

        ChunkMiner miner = minerManager.getMiner(event.getClickedBlock().getLocation());
        if (miner == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.getUniqueId().equals(miner.getOwnerUUID()) &&
                    !player.hasPermission("zadditions.chunkminer.access.others")) {
                player.sendMessage("§cNo puedes acceder al inventario del Chunk Miner de otro jugador.");
                return;
            }
            miner.openStorage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        ChunkMiner miner = minerManager.getMiner(event.getBlock().getLocation());
        if (miner == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.getUniqueId().equals(miner.getOwnerUUID()) &&
                !player.hasPermission("zadditions.chunkminer.admin")) {
            player.sendMessage("§cSolo el dueño o un administrador puede romper este Chunk Miner.");
            return;
        }

        if (!player.hasPermission("zadditions.chunkminer.place")) {
            player.sendMessage("§cNo tienes permiso para usar Chunk Miners.");
            return;
        }

        // Soltar el item al suelo
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                ChunkMinerItem.create()
        );

        // Remover el miner sin soltar item adicional
        miner.remove(false);
        minerManager.removeMiner(event.getBlock().getLocation());
        player.sendMessage("§aChunk Miner removido.");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith("§6Chunk Miner Storage")) {
            // Cancelar el drag si incluye slots de la última fila
            for (int slot : event.getRawSlots()) {
                if (slot >= 45 && slot <= 53) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
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
}