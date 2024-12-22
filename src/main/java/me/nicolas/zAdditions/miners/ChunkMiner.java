package me.nicolas.zAdditions.miners;

import me.nicolas.zAdditions.ZAdditions;
import me.nicolas.zAdditions.items.ChunkMinerItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChunkMiner {
    private final Location location;
    private final Chunk chunk;
    private UUID ownerUUID;
    private final ArmorStand hologram;
    private final ArmorStand progressHologram;
    private BukkitTask miningTask;
    private int currentX;
    private int currentY;
    private int currentZ;
    private int blocksMinedCount;
    private boolean isActive;
    private final Inventory storage;
    private Block currentBlock;
    private int miningProgress;
    private int animationFrame = 0;
    private int tickCounter = 0;

    private static final int STORAGE_SIZE = 54;
    private static final int MINING_TICKS = 10; // Medio segundo (10 ticks)
    private static final String[] ANIMATION_FRAMES = {
            "§e⚒ §6Minando §e⚒",
            "§e⚒ §6Minando. §e⚒",
            "§e⚒ §6Minando.. §e⚒",
            "§e⚒ §6Minando... §e⚒"
    };
    private static final String PROGRESS_BAR_CHAR = "█";
    private static final int PROGRESS_BAR_LENGTH = 20;

    public ChunkMiner(Location location, UUID ownerUUID) {
        this.location = location;
        this.chunk = location.getChunk();
        this.ownerUUID = ownerUUID;
        this.currentX = chunk.getX() * 16;
        this.currentY = chunk.getWorld().getMaxHeight() - 1; // Empezar desde arriba
        this.currentZ = chunk.getZ() * 16;
        this.blocksMinedCount = 0;
        this.isActive = true;
        this.storage = Bukkit.createInventory(null, STORAGE_SIZE, "§6Chunk Miner Storage");
        this.miningProgress = 0;

        // Limpiar hologramas existentes
        location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(entity -> {
            if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                entity.remove();
            }
        });

        // Crear hologramas
        this.hologram = (ArmorStand) location.getWorld().spawnEntity(
                location.clone().add(0.5, 2.2, 0.5),
                EntityType.ARMOR_STAND
        );
        this.progressHologram = (ArmorStand) location.getWorld().spawnEntity(
                location.clone().add(0.5, 1.9, 0.5),
                EntityType.ARMOR_STAND
        );
        setupHologram();
        startMining();
    }

    private void setupHologram() {
        setupArmorStand(hologram);
        setupArmorStand(progressHologram);
        updateHologram();
    }

    private void setupArmorStand(ArmorStand stand) {
        stand.setVisible(false);
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
    }

    private void updateHologram() {
        if (currentBlock != null) {
            // Actualizar animación del título
            hologram.setCustomName(ANIMATION_FRAMES[animationFrame]);
            animationFrame = (animationFrame + 1) % ANIMATION_FRAMES.length;

            // Actualizar barra de progreso
            String blockName = formatBlockName(currentBlock.getType().name());
            double progress = (miningProgress / (double) MINING_TICKS);
            String progressBar = createProgressBar(progress);

            // Mostrar coordenadas Y actual
            progressHologram.setCustomName(String.format("§7%s §8| %s §8| §eY: %d",
                    blockName, progressBar, currentY));
        } else {
            // Mostrar estadísticas generales cuando no está minando
            int totalBlocks = (chunk.getWorld().getMaxHeight() - chunk.getWorld().getMinHeight()) * 256;
            double totalProgress = (double) blocksMinedCount / totalBlocks * 100;

            hologram.setCustomName(String.format("§6§lChunk Miner"));
            progressHologram.setCustomName(String.format("§e%d §7bloques minados §8| §e%.1f%% §8| §eY: %d",
                    blocksMinedCount, totalProgress, currentY));
        }
    }

    private String formatBlockName(String name) {
        return Arrays.stream(name.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String createProgressBar(double progress) {
        int filledBars = (int) (PROGRESS_BAR_LENGTH * progress);
        StringBuilder bar = new StringBuilder();

        // Parte llena de la barra
        bar.append("§a");
        for (int i = 0; i < filledBars; i++) {
            bar.append(PROGRESS_BAR_CHAR);
        }

        // Parte vacía de la barra
        bar.append("§7");
        for (int i = filledBars; i < PROGRESS_BAR_LENGTH; i++) {
            bar.append(PROGRESS_BAR_CHAR);
        }

        return bar.toString();
    }

    private void startMining() {
        miningTask = Bukkit.getScheduler().runTaskTimer(ZAdditions.getInstance(), () -> {
            if (!isActive || !isOwnerOnline()) {
                pause();
                return;
            }

            if (currentBlock == null) {
                findNextBlock();
            } else {
                continueMiningBlock();
            }

            // Actualizar hologramas cada 5 ticks
            tickCounter++;
            if (tickCounter % 5 == 0) {
                updateHologram();
            }
        }, 0L, 1L);
    }

    private void findNextBlock() {
        while (currentY > chunk.getWorld().getMinHeight()) {
            Block block = chunk.getWorld().getBlockAt(currentX, currentY, currentZ);

            if (shouldMineBlock(block)) {
                currentBlock = block;
                miningProgress = 0;
                return;
            }

            moveToNextPosition();
        }

        finishMining();
    }

    private void moveToNextPosition() {
        currentX++;
        if (currentX >= chunk.getX() * 16 + 16) {
            currentX = chunk.getX() * 16;
            currentZ++;
            if (currentZ >= chunk.getZ() * 16 + 16) {
                currentZ = chunk.getZ() * 16;
                currentY--; // Bajar en lugar de subir
            }
        }
    }

    private void continueMiningBlock() {
        if (currentBlock == null) return;

        miningProgress++;

        Location blockLoc = currentBlock.getLocation().add(0.5, 0.5, 0.5);
        Location beaconLoc = location.clone().add(0.5, 0.5, 0.5);

        // Dibujar láser
        drawLaser(beaconLoc, blockLoc);

        // Partículas de minado
        if (miningProgress % 2 == 0) {
            currentBlock.getWorld().spawnParticle(
                    Particle.SMOKE,
                    blockLoc,
                    3,
                    0.2, 0.2, 0.2,
                    0
            );
        }

        // Sonido de minado
        if (miningProgress % 5 == 0) {
            currentBlock.getWorld().playSound(
                    blockLoc,
                    Sound.BLOCK_STONE_HIT,
                    0.2f,
                    1.0f
            );
        }

        if (miningProgress >= MINING_TICKS) {
            mineCurrentBlock();
        }
    }

    private void drawLaser(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += 0.5) {
            Vector point = start.toVector().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(
                    Particle.FLAME,
                    point.getX(), point.getY(), point.getZ(),
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private void mineCurrentBlock() {
        if (currentBlock == null) return;

        // Recolectar drops
        Collection<ItemStack> drops = currentBlock.getDrops();
        for (ItemStack drop : drops) {
            if (hasSpaceInStorage(drop)) {
                storage.addItem(drop);
            } else {
                currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), drop);
            }
        }

        Location blockLoc = currentBlock.getLocation().add(0.5, 0.5, 0.5);

        // Efectos de ruptura
        currentBlock.getWorld().playSound(
                blockLoc,
                Sound.BLOCK_STONE_BREAK,
                0.5f,
                1.0f
        );

        currentBlock.getWorld().spawnParticle(
                Particle.LAVA,
                blockLoc,
                8,
                0.2, 0.2, 0.2,
                0
        );

        currentBlock.getWorld().spawnParticle(
                Particle.SMOKE,
                blockLoc,
                12,
                0.2, 0.2, 0.2,
                0
        );

        currentBlock.setType(Material.AIR);
        blocksMinedCount++;
        currentBlock = null;
        miningProgress = 0;
        moveToNextPosition();
    }

    private boolean shouldMineBlock(Block block) {
        return !block.getType().isAir() &&
                !block.getType().equals(Material.BEDROCK) &&
                !block.getType().equals(Material.BEACON) &&
                !block.isLiquid() &&
                !block.getType().name().contains("SPAWNER");
    }

    private boolean isOwnerOnline() {
        return Bukkit.getPlayer(ownerUUID) != null;
    }

    private boolean hasSpaceInStorage(ItemStack item) {
        return storage.firstEmpty() != -1 || storage.containsAtLeast(item, item.getAmount());
    }

    public void openStorage(Player player) {
        player.openInventory(storage);
    }

    private void finishMining() {
        isActive = false;
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }

        // Efecto final
        Location loc = location.clone().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(
                Particle.EXPLOSION,
                loc,
                3,
                0.3, 0.3, 0.3,
                0
        );

        loc.getWorld().spawnParticle(
                Particle.FLAME,
                loc,
                20,
                0.5, 0.5, 0.5,
                0.1
        );

        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

        // Mensaje final en el holograma
        hologram.setCustomName("§6§l¡Completado!");
        progressHologram.setCustomName("§e" + blocksMinedCount + " §7bloques minados");

        // Programar la eliminación después de mostrar el mensaje final
        Bukkit.getScheduler().runTaskLater(ZAdditions.getInstance(), () -> {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                owner.getInventory().addItem(ChunkMinerItem.create());
                owner.sendMessage("§a¡Chunk Miner ha terminado de minar el chunk!");
            }

            cleanup();
        }, 60L);
    }

    public void cleanup() {
        isActive = false;
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }

        // Asegurarse de que el bloque se remueva
        if (location.getBlock().getType() == Material.BEACON) {
            location.getBlock().setType(Material.AIR);
        }

        // Remover hologramas de forma segura
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
        if (progressHologram != null && !progressHologram.isDead()) {
            progressHologram.remove();
        }

        // Limpiar entidades cercanas por si acaso
        location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(entity -> {
            if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                entity.remove();
            }
        });
    }

    public void remove(boolean giveItem) {
        cleanup();

        if (giveItem) {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                owner.getInventory().addItem(ChunkMinerItem.create());
            }
        }
    }

    public void pause() {
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }
    }

    public void resume() {
        if (isActive) {
            startMining();
        }
    }

    // Getters y setters
    public Location getLocation() {
        return location;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwner(UUID newOwner) {
        this.ownerUUID = newOwner;
    }

    public double getProgress() {
        int totalBlocks = (chunk.getWorld().getMaxHeight() - chunk.getWorld().getMinHeight()) * 256;
        return (double) blocksMinedCount / totalBlocks * 100;
    }

    public boolean isActive() {
        return isActive;
    }
}