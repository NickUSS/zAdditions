package me.nicolas.zAdditions.managers;

import me.nicolas.zAdditions.miners.ChunkMiner;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ChunkMinerManager {
    private final Map<Location, ChunkMiner> miners = new HashMap<>();
    private final Map<World, Integer> minersPerWorld = new HashMap<>();
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public ChunkMinerManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "miners.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("miners.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadMiners();
    }

    private void loadMiners() {
        if (!dataConfig.contains("miners")) return;

        for (String key : dataConfig.getConfigurationSection("miners").getKeys(false)) {
            try {
                String worldName = dataConfig.getString("miners." + key + ".world");
                World world = plugin.getServer().getWorld(worldName);

                if (world != null) {
                    double x = dataConfig.getDouble("miners." + key + ".x");
                    double y = dataConfig.getDouble("miners." + key + ".y");
                    double z = dataConfig.getDouble("miners." + key + ".z");
                    UUID ownerUUID = UUID.fromString(dataConfig.getString("miners." + key + ".owner"));
                    Location location = new Location(world, x, y, z);

                    if (location.getBlock().getType().equals(Material.BEACON)) {
                        addMiner(location, ownerUUID);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error al cargar Chunk Miner: " + key, e);
            }
        }
    }

    private void saveData() {
        dataConfig.set("miners", null);
        int index = 0;

        for (Map.Entry<Location, ChunkMiner> entry : miners.entrySet()) {
            Location loc = entry.getKey();
            ChunkMiner miner = entry.getValue();

            String path = "miners." + index;
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
            dataConfig.set(path + ".owner", miner.getOwnerUUID().toString());
            dataConfig.set(path + ".progress", miner.getProgress());

            index++;
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar los datos de Chunk Miners", e);
        }
    }

    public boolean canPlaceMiner(World world) {
        int worldLimit = plugin.getConfig().getInt("world-limit", 10);
        return minersPerWorld.getOrDefault(world, 0) < worldLimit;
    }

    public void addMiner(Location location, UUID ownerUUID) {
        ChunkMiner miner = new ChunkMiner(location, ownerUUID);
        miners.put(location, miner);
        minersPerWorld.merge(location.getWorld(), 1, Integer::sum);
        saveData();
    }

    public void removeMiner(Location location) {
        ChunkMiner miner = miners.remove(location);
        if (miner != null) {
            miner.remove(true);
            minersPerWorld.merge(location.getWorld(), -1, Integer::sum);
            saveData();
        }
    }

    public boolean hasMinerInChunk(Chunk chunk) {
        return miners.values().stream()
                .anyMatch(miner -> miner.getChunk().equals(chunk));
    }

    public ChunkMiner getMiner(Location location) {
        return miners.get(location);
    }

    public List<ChunkMiner> getMinersByOwner(UUID ownerUUID) {
        return miners.values().stream()
                .filter(miner -> miner.getOwnerUUID().equals(ownerUUID))
                .toList();
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

    public Map<World, Integer> getMinersCount() {
        return new HashMap<>(minersPerWorld);
    }

    public void checkAndCleanInvalidMiners() {
        List<Location> toRemove = new ArrayList<>();

        for (Map.Entry<Location, ChunkMiner> entry : miners.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null || !loc.getChunk().isLoaded() ||
                    !loc.getBlock().getType().equals(Material.BEACON)) {
                toRemove.add(loc);
            }
        }

        toRemove.forEach(this::removeMiner);
    }

    public void transferOwnership(Location minerLocation, UUID newOwner) {
        ChunkMiner miner = miners.get(minerLocation);
        if (miner != null) {
            miner.setOwner(newOwner);
            saveData();
        }
    }

    public int getMinersInWorld(World world) {
        return minersPerWorld.getOrDefault(world, 0);
    }

    public boolean isChunkMinerLocation(Location location) {
        return miners.containsKey(location);
    }

    public void saveAllData() {
        saveData();
    }

    public void reloadData() {
        miners.clear();
        minersPerWorld.clear();
        loadData();
    }

    public Set<Location> getAllMinerLocations() {
        return new HashSet<>(miners.keySet());
    }

    public boolean hasReachedGlobalLimit() {
        int globalLimit = plugin.getConfig().getInt("global-miner-limit", 50);
        return miners.size() >= globalLimit;
    }

    public void cleanup() {
        pauseAllMiners();
        saveData();
        miners.clear();
        minersPerWorld.clear();
    }
}