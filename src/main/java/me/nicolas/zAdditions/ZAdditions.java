package me.nicolas.zAdditions;

import me.nicolas.zAdditions.commands.ChunkMinerCommand;
import me.nicolas.zAdditions.listeners.ChunkMinerListener;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ZAdditions extends JavaPlugin {
    private static ZAdditions instance;
    private ChunkMinerManager chunkMinerManager;

    @Override
    public void onEnable() {
        instance = this;

        // Crear carpeta de configuración si no existe
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Guardar configuración por defecto
        saveDefaultConfig();

        // Inicializar el manager
        this.chunkMinerManager = new ChunkMinerManager(this);

        // Registrar comando y listener
        ChunkMinerCommand command = new ChunkMinerCommand(chunkMinerManager);
        ChunkMinerListener listener = new ChunkMinerListener(chunkMinerManager);

        getCommand("chunkminer").setExecutor(command);
        getServer().getPluginManager().registerEvents(listener, this);

        // Programar guardado automático
        getServer().getScheduler().runTaskTimer(this,
                () -> chunkMinerManager.saveData(),
                6000L,
                6000L
        );

        getLogger().info("ZAdditions ha sido habilitado!");
    }

    @Override
    public void onDisable() {
        if (chunkMinerManager != null) {
            chunkMinerManager.saveData();
            chunkMinerManager.cleanupAllHolograms();
        }
        getLogger().info("ZAdditions ha sido deshabilitado!");
    }

    public static ZAdditions getInstance() {
        return instance;
    }

    public ChunkMinerManager getChunkMinerManager() {
        return chunkMinerManager;
    }
}