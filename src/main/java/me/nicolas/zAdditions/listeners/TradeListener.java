package me.nicolas.zAdditions.listeners;

import me.nicolas.zAdditions.managers.TradeManager;
import me.nicolas.zAdditions.models.TradeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class TradeListener implements Listener {
    private final TradeManager tradeManager;

    public TradeListener(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());

        if (session != null) {
            session.handleInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        tradeManager.handleInventoryClose(player);
    }
}
