package me.nicolas.zAdditions.commands;

import me.nicolas.zAdditions.managers.TradeManager;
import me.nicolas.zAdditions.utils.TradeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {
    private final TradeManager tradeManager;

    public TradeCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!TradeUtils.isPlayer(sender)) {
            sender.sendMessage(TradeUtils.color("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(TradeUtils.color("&cUso: /trade <jugador>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (!TradeUtils.isValidTarget(player, target)) {
            return true;
        }

        tradeManager.handleTradeRequest(player, target);
        return true;
    }
}
