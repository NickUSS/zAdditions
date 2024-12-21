package me.nicolas.zAdditions;

import me.nicolas.zAdditions.commands.ChunkMinerCommand;
import me.nicolas.zAdditions.commands.TradeCommand;
import me.nicolas.zAdditions.listeners.ChunkMinerListener;
import me.nicolas.zAdditions.listeners.TradeListener;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import me.nicolas.zAdditions.managers.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ZAdditions extends JavaPlugin {

    private static ZAdditions instance;
    private TradeManager tradeManager;
    private ChunkMinerManager chunkMinerManager;


    @Override
    public void onEnable() {
        instance = this;

        // Inicializar el manager
        tradeManager = new TradeManager(this);
        chunkMinerManager = new ChunkMinerManager();

        // Registrar comando
        getCommand("trade").setExecutor(new TradeCommand(tradeManager));
        getCommand("chunkminer").setExecutor(new ChunkMinerCommand());


        // Registrar listener
        getServer().getPluginManager().registerEvents(
                new TradeListener(tradeManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new ChunkMinerListener(chunkMinerManager),
                this
        );

        getLogger().info("ZAdditions ha sido habilitado!");
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAllTrades();
        }

        if (chunkMinerManager != null) {
            chunkMinerManager.removeAllMiners();
        }

        getLogger().info("ZAdditions ha sido deshabilitado!");
    }

    public static ZAdditions getInstance() {
        return instance;
    }
}