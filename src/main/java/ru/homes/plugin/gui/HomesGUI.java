package ru.homes.plugin.gui;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomesGUI {

    public static final String TITLE = ChatColor.DARK_AQUA + "Твои хомы";

    public static Inventory build(Player player, HomeManager homeManager) {
        Map<String, Home> homes = homeManager.getHomes(player.getUniqueId());

        int size = Math.min(54, Math.max(9, ((homes.size() - 1) / 9 + 1) * 9));
        List<String> order = new ArrayList<>(homes.keySet());

        HomesInventoryHolder holder = new HomesInventoryHolder(order);
        Inventory inv = Bukkit.createInventory(holder, size, TITLE);
        holder.setInventory(inv);

        int slot = 0;
        for (String name : order) {
            if (slot >= size) break; // больше 54 хомов не влезет в один GUI
            Home home = homes.get(name);
            inv.setItem(slot, buildItem(name, home));
            slot++;
        }

        return inv;
    }

    private static ItemStack buildItem(String name, Home home) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Мир: " + ChatColor.WHITE + home.getWorld());
        lore.add(ChatColor.GRAY + String.format("X: %.0f  Y: %.0f  Z: %.0f",
                home.getX(), home.getY(), home.getZ()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Клик — телепортация");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
