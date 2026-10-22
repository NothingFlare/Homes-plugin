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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.language.LanguageManager;
import ru.homes.plugin.teleport.HomeTeleportManager;

public class HomesGUIListener implements Listener {

    private final HomeManager homeManager;
    private final HomeTeleportManager teleportManager;

    public HomesGUIListener(HomeManager homeManager, HomeTeleportManager teleportManager) {
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof HomesInventoryHolder mainHolder) {
            handleMainMenuClick(event, mainHolder);
        } else if (event.getInventory().getHolder() instanceof HomeEditorInventoryHolder editorHolder) {
            handleEditorMenuClick(event, editorHolder);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, HomesInventoryHolder holder) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        LanguageManager lm = homeManager.getPlugin().getLanguageManager();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.BARRIER) {
            int homeLimit = homeManager.getPlugin().getConfig().getInt("home-limit", 10);
            lm.sendMessage(player, "gui.slot-blocked", "limit", homeLimit);
            return;
        }

        if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        String homeName = holder.getHomeNameAt(event.getSlot());
        if (homeName == null) return;

        // ПКМ — открыть редактор хома
        if (event.isRightClick()) {
            Home home = homeManager.getHome(player.getUniqueId(), homeName);
            if (home != null) {
                HomeEditorGUI.open(player, homeManager.getPlugin(), homeName);
            } else {
                lm.sendMessage(player, "gui.home-lost", "name", homeName);
            }
            return;
        }

        // ЛКМ (или обычный клик) — телепортация к хому
        if (!player.hasPermission("homes.home")) {
            lm.sendMessage(player, "teleport.no-permission");
            player.closeInventory();
            return;
        }

        Home home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            lm.sendMessage(player, "gui.home-lost", "name", homeName);
            return;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            lm.sendMessage(player, "home.world-not-loaded", "name", homeName, "world", home.getWorld());
            return;
        }

        player.closeInventory();
        int timeout = homeManager.getPlugin().getConfig().getInt("teleport-timeout", 5);
        teleportManager.teleport(player, loc, homeName, timeout);
    }

    private void handleEditorMenuClick(InventoryClickEvent event, HomeEditorInventoryHolder holder) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        LanguageManager lm = homeManager.getPlugin().getLanguageManager();
        String homeName = holder.getHomeName();
        int slot = event.getSlot();

        if (slot == HomeEditorGUI.SLOT_BACK) {
            HomesGUI.open(player, homeManager.getPlugin());
            return;
        }

        if (slot == HomeEditorGUI.SLOT_TELEPORT) {
            if (!player.hasPermission("homes.home")) {
                lm.sendMessage(player, "teleport.no-permission");
                player.closeInventory();
                return;
            }

            Home home = homeManager.getHome(player.getUniqueId(), homeName);
            if (home == null) {
                lm.sendMessage(player, "gui.home-lost", "name", homeName);
                HomesGUI.open(player, homeManager.getPlugin());
                return;
            }

            Location loc = home.toLocation();
            if (loc == null) {
                lm.sendMessage(player, "home.world-not-loaded", "name", homeName, "world", home.getWorld());
                return;
            }

            player.closeInventory();
            int timeout = homeManager.getPlugin().getConfig().getInt("teleport-timeout", 5);
            teleportManager.teleport(player, loc, homeName, timeout);
            return;
        }

        if (slot == HomeEditorGUI.SLOT_DELETE) {
            if (!player.hasPermission("homes.delhome")) {
                lm.sendMessage(player, "delhome.no-permission");
                return;
            }

            boolean removed = homeManager.delHome(player.getUniqueId(), homeName);
            if (removed) {
                lm.sendMessage(player, "delhome.deleted", "name", homeName);
                HomesGUI.open(player, homeManager.getPlugin());
            } else {
                lm.sendMessage(player, "delhome.not-found", "name", homeName);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HomesInventoryHolder
                || event.getInventory().getHolder() instanceof HomeEditorInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
