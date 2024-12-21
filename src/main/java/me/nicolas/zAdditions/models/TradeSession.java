package me.nicolas.zAdditions.models;

import me.nicolas.zAdditions.utils.TradeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class TradeSession {
    private final Player player1;
    private final Player player2;
    private final Inventory tradeInventory;
    private boolean player1Ready;
    private boolean player2Ready;

    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.tradeInventory = createTradeInventory();
        this.player1Ready = false;
        this.player2Ready = false;
    }

    private Inventory createTradeInventory() {
        Inventory inv = Bukkit.createInventory(null, 54, "Trade: " + player1.getName() + " - " + player2.getName());
        TradeUtils.setupTradeInventory(inv);
        return inv;
    }

    public void openInventories() {
        player1.openInventory(tradeInventory);
        player2.openInventory(tradeInventory);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (TradeUtils.isAcceptButton(event.getCurrentItem())) {
            event.setCancelled(true);
            toggleReady(player);
            return;
        }

        if (!TradeUtils.isValidTradeSlot(player, this, slot)) {
            event.setCancelled(true);
        }
    }

    private void toggleReady(Player player) {
        if (player.equals(player1)) {
            player1Ready = !player1Ready;
        } else {
            player2Ready = !player2Ready;
        }

        if (player1Ready && player2Ready) {
            completeTrade();
        }
    }

    public void completeTrade() {
        TradeUtils.transferItems(tradeInventory, player1, player2);
        player1.sendMessage(TradeUtils.color("&a¡Trade completado!"));
        player2.sendMessage(TradeUtils.color("&a¡Trade completado!"));
        closeInventories();
    }

    public void cancelTrade() {
        TradeUtils.returnItems(tradeInventory, player1, player2);
        player1.sendMessage(TradeUtils.color("&cTrade cancelado."));
        player2.sendMessage(TradeUtils.color("&cTrade cancelado."));
        closeInventories();
    }

    private void closeInventories() {
        player1.closeInventory();
        player2.closeInventory();
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }
}