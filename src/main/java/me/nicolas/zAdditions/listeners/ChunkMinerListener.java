package me.nicolas.zAdditions.listeners;

import me.nicolas.zAdditions.ZAdditions;
import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import me.nicolas.zAdditions.miners.ChunkMiner;
import me.nicolas.zAdditions.utils.WorldGuardUtils;
import org.bukkit.Location;
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

import java.util.HashMap;

public class ChunkMinerListener implements Listener {
    private final ChunkMinerManager minerManager;
    private final ZAdditions plugin;

    public ChunkMinerListener(ChunkMinerManager minerManager) {
        if (minerManager == null) {
            throw new IllegalArgumentException("ChunkMinerManager no puede ser null");
        }
        this.minerManager = minerManager;
        this.plugin = ZAdditions.getInstance();
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ChunkMinerItem.isChunkMiner(event.getItemInHand())) {
            return;
        }

        Player player = event.getPlayer();

        // Verificar permiso básico
        if (!player.hasPermission("zadditions.chunkminer.place")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        // Verificar si ya hay un miner en este chunk
        if (minerManager.hasMinerInChunk(event.getBlock().getChunk())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage("chunk-occupied"));
            return;
        }

        // Verificar límites de jugador y mundo
        if (!minerManager.canPlaceMiner(event.getBlock().getWorld(), player.getUniqueId())) {
            event.setCancelled(true);
            int currentMiners = minerManager.getPlayerMinersCount(player.getUniqueId());
            if (currentMiners >= 3) {
                player.sendMessage("§cHas alcanzado el límite máximo de Chunk Miners (3).");
            } else {
                player.sendMessage(plugin.getMessage("world-limit"));
            }
            return;
        }

        // Verificar restricciones de WorldGuard (solo si está habilitado)
        if (plugin.isWorldGuardEnabled()) {
            // Verificar las 4 esquinas y centro del chunk
            int chunkX = event.getBlock().getChunk().getX() * 16;
            int chunkZ = event.getBlock().getChunk().getZ() * 16;
            int y = event.getBlock().getWorld().getMaxHeight() / 2;

            Location[] testPoints = {
                    new Location(event.getBlock().getWorld(), chunkX, y, chunkZ),
                    new Location(event.getBlock().getWorld(), chunkX + 15, y, chunkZ),
                    new Location(event.getBlock().getWorld(), chunkX, y, chunkZ + 15),
                    new Location(event.getBlock().getWorld(), chunkX + 15, y, chunkZ + 15),
                    new Location(event.getBlock().getWorld(), chunkX + 8, y, chunkZ + 8)
            };

            boolean canPlaceInChunk = true;

            for (Location point : testPoints) {
                // Verificar si el punto está en una región protegida
                if (WorldGuardUtils.isInGlobalRegion(point.getBlock())) {
                    // Si está en región global (sin protección), permitir
                    continue;
                }

                // Si está en una región protegida, verificar si el jugador es dueño
                if (!WorldGuardUtils.isOwner(player, point)) {
                    canPlaceInChunk = false;
                    break;
                }
            }

            if (!canPlaceInChunk) {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes colocar un Chunk Miner aquí porque hay regiones protegidas en este chunk que no te pertenecen.");
                return;
            }
        }

        // Si todas las verificaciones pasan, añadir el miner
        minerManager.addMiner(event.getBlock().getLocation(), player.getUniqueId());
        player.sendMessage(plugin.getMessage("placed") + " §7(§e" +
                minerManager.getPlayerMinersCount(player.getUniqueId()) + "§7/§e3§7)");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        ChunkMiner miner = minerManager.getMiner(event.getBlock().getLocation());
        if (miner == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Verificar si es el dueño
        boolean isOwner = player.getUniqueId().equals(miner.getOwnerUUID());

        // Permitir que el dueño o un admin rompa el ChunkMiner
        if (!isOwner && !player.hasPermission("zadditions.chunkminer.admin")) {
            player.sendMessage("§cSolo el dueño o un administrador puede romper este Chunk Miner.");
            return;
        }

        // Verificar permiso básico (esto es para los admins)
        if (!isOwner && !player.hasPermission("zadditions.chunkminer.place")) {
            player.sendMessage("§cNo tienes permiso para usar Chunk Miners.");
            return;
        }

        // Entregar el item directamente al inventario del jugador si es el dueño
        if (isOwner) {
            // Crear el item de ChunkMiner
            ItemStack chunkMinerItem = ChunkMinerItem.create();

            // Intentar añadir al inventario
            HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(chunkMinerItem);

            // Si no se pudo añadir al inventario (porque está lleno), soltarlo al suelo
            if (!notAdded.isEmpty()) {
                player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        chunkMinerItem
                );
                player.sendMessage("§aTu inventario está lleno. El Chunk Miner ha sido soltado a tus pies.");
            } else {
                player.sendMessage("§aChunk Miner recuperado a tu inventario.");
            }
        } else {
            // Si es un admin quien lo rompe, soltar el item al suelo como antes
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    ChunkMinerItem.create()
            );
            player.sendMessage("§aChunk Miner removido y soltado al suelo.");
        }

        // Remover el miner sin soltar item adicional
        miner.remove(false);
        minerManager.removeMiner(event.getBlock().getLocation());
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
}