package me.nicolas.zAdditions.managers;

import me.nicolas.zAdditions.miners.ChunkMiner;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class ChunkMinerManager {
    private final Map<Location, ChunkMiner> miners = new HashMap<>();
    private final Map<World, Integer> minersPerWorld = new HashMap<>();

    public boolean canPlaceMiner(World world) {
        return minersPerWorld.getOrDefault(world, 0) < 10;
    }

    public void addMiner(Location location, UUID ownerUUID) {
        ChunkMiner miner = new ChunkMiner(location, ownerUUID);
        miners.put(location, miner);
        minersPerWorld.merge(location.getWorld(), 1, Integer::sum);
    }

    public void removeMiner(Location location) {
        ChunkMiner miner = miners.remove(location);
        if (miner != null) {
            miner.remove();
            minersPerWorld.merge(location.getWorld(), -1, Integer::sum);
        }
    }

    public boolean hasMinerInChunk(Chunk chunk) {
        return miners.values().stream()
                .anyMatch(miner -> miner.getChunk().equals(chunk));
    }

    public ChunkMiner getMiner(Location location) {
        return miners.get(location);
    }

    public void pauseAllMiners() {
        miners.values().forEach(ChunkMiner::pause);
    }

    public void resumeAllMiners() {
        miners.values().forEach(ChunkMiner::resume);
    }

    public void removeAllMiners() {
        new ArrayList<>(miners.keySet()).forEach(this::removeMiner);
    }
}