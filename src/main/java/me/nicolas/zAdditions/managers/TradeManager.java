package me.nicolas.zAdditions.managers;

import me.nicolas.zAdditions.models.TradeSession;
import me.nicolas.zAdditions.utils.TradeUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {
    private final Plugin plugin;
    private final Map<UUID, UUID> tradeRequests;
    private final Map<UUID, TradeSession> tradeSessions;

    public TradeManager(Plugin plugin) {
        this.plugin = plugin;
        this.tradeRequests = new HashMap<>();
        this.tradeSessions = new HashMap<>();
    }

    public void handleTradeRequest(Player sender, Player target) {
        if (tradeRequests.containsKey(target.getUniqueId()) &&
                tradeRequests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
            // Aceptar trade
            createTradeSession(sender, target);
            tradeRequests.remove(target.getUniqueId());
        } else {
            // Enviar nueva solicitud
            sendTradeRequest(sender, target);
        }
    }

    private void sendTradeRequest(Player sender, Player target) {
        if (tradeRequests.containsKey(sender.getUniqueId())) {
            sender.sendMessage(TradeUtils.color("&cYa tienes una solicitud pendiente."));
            return;
        }

        tradeRequests.put(sender.getUniqueId(), target.getUniqueId());
        sender.sendMessage(TradeUtils.color("&aSolicitud enviada a " + target.getName()));
        target.sendMessage(TradeUtils.color("&a" + sender.getName() + " quiere tradear contigo. Usa /trade " + sender.getName() + " para aceptar."));

        // Expirar solicitud después de 30 segundos
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                expireTradeRequest(sender.getUniqueId(), target), 600L);
    }

    private void createTradeSession(Player player1, Player player2) {
        TradeSession session = new TradeSession(player1, player2);
        tradeSessions.put(player1.getUniqueId(), session);
        tradeSessions.put(player2.getUniqueId(), session);
        session.openInventories();
    }

    public TradeSession getTradeSession(UUID playerUuid) {
        return tradeSessions.get(playerUuid);
    }

    public void handleInventoryClose(Player player) {
        TradeSession session = getTradeSession(player.getUniqueId());
        if (session != null) {
            session.cancelTrade();
            removeTradeSession(session);
        }
    }

    private void expireTradeRequest(UUID senderUuid, Player target) {
        if (tradeRequests.remove(senderUuid) != null) {
            Player sender = plugin.getServer().getPlayer(senderUuid);
            if (sender != null) {
                sender.sendMessage(TradeUtils.color("&cLa solicitud ha expirado."));
            }
            target.sendMessage(TradeUtils.color("&cLa solicitud de trade ha expirado."));
        }
    }

    public void removeTradeSession(TradeSession session) {
        tradeSessions.remove(session.getPlayer1().getUniqueId());
        tradeSessions.remove(session.getPlayer2().getUniqueId());
    }

    public void cancelAllTrades() {
        tradeSessions.values().forEach(TradeSession::cancelTrade);
        tradeSessions.clear();
        tradeRequests.clear();
    }
}
