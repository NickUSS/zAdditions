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
        chunkMinerManager = new ChunkMinerManager(this);

        // Registrar comandos
        getCommand("chunkminer").setExecutor(new ChunkMinerCommand());

        // Registrar eventos
        getServer().getPluginManager().registerEvents(new ChunkMinerListener(chunkMinerManager), this);

        // Programar guardado automático
        getServer().getScheduler().runTaskTimer(this,
                () -> chunkMinerManager.saveAllData(),
                6000L, // 5 minutos
                6000L  // 5 minutos
        );

        getLogger().info("ZAdditions ha sido habilitado!");
    }

    @Override
    public void onDisable() {
        if (chunkMinerManager != null) {
            chunkMinerManager.cleanup();
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