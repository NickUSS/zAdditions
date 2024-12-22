package me.nicolas.zAdditions.commands;

import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.managers.ChunkMinerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChunkMinerCommand implements CommandExecutor, TabCompleter {
    private final ChunkMinerManager minerManager;

    public ChunkMinerCommand(ChunkMinerManager minerManager) {
        this.minerManager = minerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reset":
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage("§cEste comando solo puede ser ejecutado desde la consola.");
                        return true;
                    }
                    minerManager.resetAllMiners();
                    sender.sendMessage("§aTodos los Chunk Miners y hologramas han sido eliminados.");
                    return true;

                case "pause":
                    if (!sender.hasPermission("zadditions.chunkminer.manage")) {
                        sender.sendMessage("§cNo tienes permiso para pausar Chunk Miners.");
                        return true;
                    }
                    minerManager.pauseAllMiners();
                    Bukkit.broadcastMessage("§e¡Todos los Chunk Miners han sido pausados!");
                    return true;

                case "start":
                    if (!sender.hasPermission("zadditions.chunkminer.manage")) {
                        sender.sendMessage("§cNo tienes permiso para iniciar Chunk Miners.");
                        return true;
                    }
                    minerManager.resumeAllMiners();
                    Bukkit.broadcastMessage("§a¡Todos los Chunk Miners han sido reactivados!");
                    return true;

                case "give":
                    if (!sender.hasPermission("zadditions.chunkminer.give.others")) {
                        sender.sendMessage("§cNo tienes permiso para dar Chunk Miners a otros jugadores.");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage("§cUso: /chunkminer give <jugador> [cantidad]");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cJugador no encontrado.");
                        return true;
                    }

                    int amount = 1;
                    if (args.length >= 3) {
                        try {
                            amount = Integer.parseInt(args[2]);
                            if (amount < 1 || amount > 64) {
                                sender.sendMessage("§cLa cantidad debe estar entre 1 y 64.");
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cLa cantidad debe ser un número válido.");
                            return true;
                        }
                    }

                    ItemStack minerItem = ChunkMinerItem.create();
                    minerItem.setAmount(amount);
                    target.getInventory().addItem(minerItem);
                    target.sendMessage("§aHas recibido " + amount + " Chunk Miner" + (amount > 1 ? "s" : "") + ".");
                    sender.sendMessage("§aHas dado " + amount + " Chunk Miner" + (amount > 1 ? "s" : "") + " a " + target.getName() + ".");
                    return true;
            }
        }

        // Comando para darse a sí mismo
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("zadditions.chunkminer.give")) {
            player.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        int amount = 1;
        if (args.length > 0) {
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
            if (sender instanceof ConsoleCommandSender) {
                completions.add("reset");
            }
            if (sender.hasPermission("zadditions.chunkminer.manage")) {
                completions.add("pause");
                completions.add("start");
            }
            if (sender.hasPermission("zadditions.chunkminer.give.others")) {
                completions.add("give");
            }
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") &&
                sender.hasPermission("zadditions.chunkminer.give.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") &&
                sender.hasPermission("zadditions.chunkminer.give.others")) {
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        return completions;
    }
}