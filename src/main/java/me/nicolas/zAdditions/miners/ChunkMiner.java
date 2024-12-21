package me.nicolas.zAdditions.miners;

import me.nicolas.zAdditions.ZAdditions;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class ChunkMiner {
    private final Location location;
    private final Chunk chunk;
    private final UUID ownerUUID;
    private final ArmorStand hologram;
    private BukkitTask miningTask;
    private int currentY;
    private int blocksMinedCount;
    private boolean isActive;

    public ChunkMiner(Location location, UUID ownerUUID) {
        this.location = location;
        this.chunk = location.getChunk();
        this.ownerUUID = ownerUUID;
        this.currentY = chunk.getWorld().getMinHeight();
        this.blocksMinedCount = 0;
        this.isActive = true;

        // Crear holograma
        this.hologram = (ArmorStand) location.getWorld().spawnEntity(
                location.clone().add(0.5, 2, 0.5),
                EntityType.ARMOR_STAND
        );
        setupHologram();
        startMining();
    }

    private void setupHologram() {
        hologram.setVisible(false);
        hologram.setCustomNameVisible(true);
        hologram.setGravity(false);
        hologram.setMarker(true);
        updateHologram();
    }

    private void updateHologram() {
        hologram.setCustomName(String.format("§6Bloques minados: §e%d", blocksMinedCount));
    }

    private void startMining() {
        miningTask = Bukkit.getScheduler().runTaskTimer(ZAdditions.getInstance(), () -> {
            if (!isActive || !isOwnerOnline()) {
                pause();
                return;
            }

            mineNextBlock();
        }, 0L, 60L); // 3 segundos (60 ticks)
    }

    private boolean isOwnerOnline() {
        return Bukkit.getPlayer(ownerUUID) != null;
    }

    private void mineNextBlock() {
        if (currentY >= location.getWorld().getMaxHeight()) {
            remove();
            return;
        }

        for (int x = chunk.getX() * 16; x < (chunk.getX() * 16) + 16; x++) {
            for (int z = chunk.getZ() * 16; z < (chunk.getZ() * 16) + 16; z++) {
                Block block = chunk.getWorld().getBlockAt(x, currentY, z);
                if (shouldMineBlock(block)) {
                    mineBlock(block);
                    return;
                }
            }
        }
        currentY++;
    }

    private boolean shouldMineBlock(Block block) {
        return !block.getType().isAir() &&
                block.getType() != Material.BEDROCK &&
                block.getType() != Material.BEACON;
    }

    private void mineBlock(Block block) {
        Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);

        // Efecto de partículas
        block.getWorld().spawnParticle(
                Particle.DUST,
                blockLoc,
                10,
                0.3, 0.3, 0.3,
                0,
                new Particle.DustOptions(Color.RED, 1)
        );

        // Efecto de sonido
        block.getWorld().playSound(
                blockLoc,
                Sound.BLOCK_STONE_BREAK,
                1.0f,
                1.0f
        );

        block.setType(Material.AIR);
        blocksMinedCount++;
        updateHologram();
    }

    public void pause() {
        if (miningTask != null) {
            miningTask.cancel();
        }
    }

    public void resume() {
        if (isActive) {
            startMining();
        }
    }

    public void remove() {
        isActive = false;
        if (miningTask != null) {
            miningTask.cancel();
        }
        hologram.remove();
        location.getBlock().setType(Material.AIR);
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Chunk getChunk() {
        return chunk;
    }
}
