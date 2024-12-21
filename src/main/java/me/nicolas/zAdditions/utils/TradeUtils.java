package me.nicolas.zAdditions.utils;

import me.nicolas.zAdditions.models.TradeSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TradeUtils {

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    public static boolean isValidTarget(Player sender, Player target) {
        if (target == null) {
            sender.sendMessage(color("&cJugador no encontrado."));
            return false;
        }
        if (target.equals(sender)) {
            sender.sendMessage(color("&cNo puedes tradear contigo mismo."));
            return false;
        }
        return true;
    }

    public static void setupTradeInventory(Inventory inventory) {
        // Separador central
        ItemStack separator = createItem(Material.BLACK_STAINED_GLASS_PANE, "&8|");
        for (int i = 4; i < 54; i += 9) {
            inventory.setItem(i, separator);
        }

        // Botones de aceptar
        ItemStack acceptButton = createItem(Material.GREEN_WOOL, "&aClick para aceptar");
        inventory.setItem(49, acceptButton);
    }

    public static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isAcceptButton(ItemStack item) {
        return item != null && item.getType() == Material.GREEN_WOOL;
    }

    public static boolean isValidTradeSlot(Player player, TradeSession session, int slot) {
        if (player.equals(session.getPlayer1())) {
            return slot <= 3;
        } else {
            return slot >= 5 && slot <= 8;
        }
    }

    public static void transferItems(Inventory inventory, Player player1, Player player2) {
        // Transferir items de player1 a player2
        for (int i = 0; i <= 3; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                player2.getInventory().addItem(item);
            }
        }

        // Transferir items de player2 a player1
        for (int i = 5; i <= 8; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                player1.getInventory().addItem(item);
            }
        }
    }

    public static void returnItems(Inventory inventory, Player player1, Player player2) {
        // Devolver items a player1
        for (int i = 0; i <= 3; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                player1.getInventory().addItem(item);
            }
        }

        // Devolver items a player2
        for (int i = 5; i <= 8; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                player2.getInventory().addItem(item);
            }
        }
    }
}