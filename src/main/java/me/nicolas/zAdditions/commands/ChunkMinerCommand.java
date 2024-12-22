package me.nicolas.zAdditions.commands;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChunkMinerCommand implements CommandExecutor, TabCompleter {

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

        if (args.length > 1) {
            player.sendMessage("§cUso: /chunkminer [cantidad]");
            return true;
        }

        int amount = 1;
        if (args.length == 1) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount < 1 || amount > 64) {
                    player.sendMessage("§cLa cantidad debe estar entre 1 y 64.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cLa cantidad debe ser un número válido.");
                return true;
            }
        }

        ItemStack chunkMiner = ChunkMinerItem.create();
        chunkMiner.setAmount(amount);
        player.getInventory().addItem(chunkMiner);
        player.sendMessage("§aHas recibido " + amount + " Chunk Miner" + (amount > 1 ? "s" : "") + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        return completions;
    }
}