/*
 * MIT License
 *
 * Copyright (c) 2026 NothingFlare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
