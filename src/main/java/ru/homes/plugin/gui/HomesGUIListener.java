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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;

public class HomesGUIListener implements Listener {

    private final HomeManager homeManager;

    public HomesGUIListener(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomesInventoryHolder holder)) {
            return;
        }

        // блокируем любое перемещение предметов из этого GUI
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        String homeName = holder.getHomeNameAt(event.getSlot());
        if (homeName == null) return;

        Home home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(ChatColor.RED + "Хом \"" + homeName + "\" больше не существует.");
            return;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Мир хома \"" + homeName + "\" (" + home.getWorld() + ") не загружен.");
            return;
        }

        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Телепортация к \"" + homeName + "\".");
    }
}
