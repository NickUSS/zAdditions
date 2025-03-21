package me.nicolas.zAdditions;

import me.nicolas.zAdditions.commands.ChunkMinerCommand;
import me.nicolas.zAdditions.listeners.ChunkMinerListener;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import me.nicolas.zAdditions.utils.WorldGuardUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ZAdditions extends JavaPlugin {
    private static ZAdditions instance;
    private ChunkMinerManager chunkMinerManager;
    private boolean worldGuardEnabled;

    @Override
    public void onEnable() {
        instance = this;

        // Crear carpeta de configuración si no existe
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Guardar configuración por defecto
        saveDefaultConfig();

        // Cargar librerías externas
        loadExternalLibraries();

        // Intentar inicializar WorldGuard
        if (getConfig().getBoolean("worldguard.enabled", true)) {
            worldGuardEnabled = WorldGuardUtils.initialize();
            if (worldGuardEnabled) {
                getLogger().info("WorldGuard encontrado e integrado correctamente.");
            } else {
                getLogger().warning("WorldGuard no encontrado. La protección de regiones no estará disponible.");
            }
        } else {
            worldGuardEnabled = false;
            getLogger().info("Integración con WorldGuard deshabilitada en la configuración.");
        }

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

    /**
     * Carga las librerías externas desde la carpeta 'libs'
     */
    private void loadExternalLibraries() {
        File libsFolder = new File(getDataFolder(), "libs");
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
            getLogger().info("Carpeta 'libs' creada. Por favor, coloca las dependencias necesarias en ella.");
        }

        // Verificar si existen los archivos .jar de WorldGuard y WorldEdit
        File worldGuardJar = new File(libsFolder, "worldguard-bukkit.jar");
        File worldEditJar = new File(libsFolder, "worldedit-bukkit.jar");

        if (!worldGuardJar.exists() || !worldEditJar.exists()) {
            getLogger().warning("No se encontraron las librerías de WorldGuard y/o WorldEdit en la carpeta 'libs'.");
            getLogger().warning("Por favor, coloca los archivos 'worldguard-bukkit.jar' y 'worldedit-bukkit.jar' en la carpeta 'plugins/ZAdditions/libs'.");
        }
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

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    /**
     * Obtiene un mensaje de la configuración con formato de color
     * @param path Ruta del mensaje en el archivo de configuración
     * @return Mensaje formateado con colores
     */
    public String getMessage(String path) {
        String message = getConfig().getString("messages." + path, "§cMensaje no encontrado: " + path);
        return message.replace("&", "§");
    }
}