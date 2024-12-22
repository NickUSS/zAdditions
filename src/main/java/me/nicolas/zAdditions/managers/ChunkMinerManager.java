package me.nicolas.zAdditions.managers;

import me.nicolas.zAdditions.miners.ChunkMiner;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChunkMinerManager implements Listener {
    private final Map<Location, ChunkMiner> miners;
    private final Map<World, Integer> minersPerWorld;
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private boolean isGloballyPaused = false;
    private final Map<UUID, Integer> minersPerPlayer = new HashMap<>();
    private static final int MAX_MINERS_PER_PLAYER = 3;

    public ChunkMinerManager(Plugin plugin) {
        this.plugin = plugin;
        this.miners = new HashMap<>();
        this.minersPerWorld = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "miners.yml");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("miners.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Limpiar contadores existentes
        minersPerPlayer.clear();
        minersPerWorld.clear();
        miners.clear();

        isGloballyPaused = dataConfig.getBoolean("globally-paused", false);

        if (dataConfig.contains("miners")) {
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

                        if (location.getBlock().getType() == Material.BEACON) {
                            ChunkMiner miner = new ChunkMiner(location, ownerUUID);
                            miners.put(location, miner);
                            minersPerWorld.merge(world, 1, Integer::sum);
                            minersPerPlayer.merge(ownerUUID, 1, Integer::sum);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al cargar Chunk Miner: " + key);
                }
            }
        }
    }

    public boolean isInitialized() {
        return plugin != null && miners != null && minersPerWorld != null;
    }

    public void validateState() {
        if (!isInitialized()) {
            throw new IllegalStateException("ChunkMinerManager no está inicializado correctamente");
        }
    }

    public boolean canPlaceMiner(World world, UUID playerUUID) {
        // Verificar límite por jugador
        if (minersPerPlayer.getOrDefault(playerUUID, 0) >= MAX_MINERS_PER_PLAYER) {
            return false;
        }
        // Verificar límite por mundo
        return minersPerWorld.getOrDefault(world, 0) < 10;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isGloballyPaused) {
            getMinersByOwner(player.getUniqueId()).forEach(ChunkMiner::resume);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        getMinersByOwner(player.getUniqueId()).forEach(ChunkMiner::pause);
    }

    public void pauseAllMiners() {
        if (isGloballyPaused) {
            throw new IllegalStateException("Los Chunk Miners ya están pausados.");
        }

        isGloballyPaused = true;
        miners.values().forEach(ChunkMiner::pause);
        saveData();
    }

    public void resumeAllMiners() {
        if (!isGloballyPaused) {
            throw new IllegalStateException("Los Chunk Miners ya están activos.");
        }

        isGloballyPaused = false;
        miners.values().forEach(miner -> {
            if (isOwnerOnline(miner.getOwnerUUID())) {
                miner.resume();
            }
        });
        saveData();
    }

    private boolean isOwnerOnline(UUID ownerUUID) {
        return plugin.getServer().getPlayer(ownerUUID) != null;
    }

    public List<ChunkMiner> getMinersByOwner(UUID ownerUUID) {
        return miners.values().stream()
                .filter(miner -> miner.getOwnerUUID().equals(ownerUUID))
                .collect(Collectors.toList());
    }

    private void loadMiners() {
        // Primero limpiar cualquier holograma existente
        cleanupAllHolograms();

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

                    if (location.getBlock().getType() == Material.BEACON) {
                        addMiner(location, ownerUUID);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar Chunk Miner: " + key);
            }
        }
    }

    public boolean hasMinerInChunk(Chunk chunk) {
        validateState();
        return miners.values().stream()
                .anyMatch(miner -> miner.getChunk().equals(chunk));
    }

    public boolean canPlaceMiner(World world) {
        validateState();
        return minersPerWorld.getOrDefault(world, 0) < 10;
    }

    public void addMiner(Location location, UUID ownerUUID) {
        ChunkMiner miner = new ChunkMiner(location, ownerUUID);
        miners.put(location, miner);
        minersPerWorld.merge(location.getWorld(), 1, Integer::sum);
        minersPerPlayer.merge(ownerUUID, 1, Integer::sum); // Incrementar contador del jugador
        saveData();
    }

    public void saveData() {
        dataConfig.set("miners", null);
        dataConfig.set("globally-paused", isGloballyPaused);

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

            index++;
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar los datos de los Chunk Miners: " + e.getMessage());
        }
    }

    public void removeMiner(Location location) {
        ChunkMiner miner = miners.remove(location);
        if (miner != null) {
            miner.remove(true);
            minersPerWorld.merge(location.getWorld(), -1, Integer::sum);
            // Decrementar contador del jugador
            UUID ownerUUID = miner.getOwnerUUID();
            minersPerPlayer.merge(ownerUUID, -1, Integer::sum);
            if (minersPerPlayer.get(ownerUUID) <= 0) {
                minersPerPlayer.remove(ownerUUID);
            }
        }
    }

    public int getPlayerMinersCount(UUID playerUUID) {
        return minersPerPlayer.getOrDefault(playerUUID, 0);
    }

    public ChunkMiner getMiner(Location location) {
        return miners.get(location);
    }

    public Collection<ChunkMiner> getAllMiners() {
        return new ArrayList<>(miners.values());
    }

    public boolean isGloballyPaused() {
        return isGloballyPaused;
    }

    public int getActiveMinersCount() {
        return (int) miners.values().stream()
                .filter(miner -> !miner.isPaused())
                .count();
    }

    public int getTotalMinersCount() {
        return miners.size();
    }

    public void cleanupAllHolograms() {
        for (World world : Bukkit.getWorlds()) {
            world.getEntities().forEach(entity -> {
                if (entity instanceof TextDisplay) {
                    TextDisplay display = (TextDisplay) entity;
                    String text = display.getText();
                    if (text != null && (text.contains("Chunk Miner") ||
                            text.contains("bloques minados") ||
                            text.contains("Minando"))) {
                        entity.remove();
                    }
                }
            });
        }
    }

    public void resetAllMiners() {
        cleanupAllHolograms();
        new ArrayList<>(miners.keySet()).forEach(loc -> {
            ChunkMiner miner = miners.get(loc);
            if (miner != null) {
                miner.cleanup();
            }
        });
        miners.clear();
        minersPerWorld.clear();

        // Limpiar y guardar la configuración
        dataConfig.set("miners", null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar la configuración después del reset: " + e.getMessage());
        }
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