package me.nicolas.zAdditions.commands;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChunkMinerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("zadditions.chunkminer.give")) {
            player.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        player.getInventory().addItem(ChunkMinerItem.create());
        player.sendMessage("§aHas recibido un Chunk Miner.");

        return true;
    }
}
