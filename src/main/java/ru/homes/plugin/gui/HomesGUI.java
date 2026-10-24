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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.language.LanguageManager;
import ru.homes.plugin.platform.PlatformAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomesGUI {

    public static final int INVENTORY_SIZE = 54; // Двойной сундук (6 рядов по 9 слотов)

    /**
     * Открывает интерфейс сразу со всеми предметами.
     */
    public static void open(Player player, HomesPlugin plugin) {
        player.openInventory(build(player, plugin.getHomeManager()));
    }

    /**
     * Создает сразу полностью заполненный инвентарь (для прямого получения).
     */
    public static Inventory build(Player player, HomeManager homeManager) {
        HomesPlugin plugin = homeManager.getPlugin();
        LanguageManager lm = plugin.getLanguageManager();
        PlatformAdapter adapter = plugin.getPlatformManager().getAdapter();

        Map<String, Home> homes = homeManager.getHomes(player.getUniqueId());
        int homeLimit = plugin.getConfig().getInt("home-limit", 10);
        boolean hasBypass = player.hasPermission("homes.bypass");

        HomesInventoryHolder holder = new HomesInventoryHolder();
        String title = lm.get("gui.main-title");
        Inventory inv = adapter.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inv);

        ItemStack[] targetItems = buildTargetItems(holder, homes, homeLimit, hasBypass, lm, adapter);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (targetItems[i] != null) {
                inv.setItem(i, targetItems[i]);
            }
        }

        return inv;
    }

    private static ItemStack[] buildTargetItems(HomesInventoryHolder holder, Map<String, Home> homes,
                                                int homeLimit, boolean hasBypass, LanguageManager lm,
                                                PlatformAdapter adapter) {
        ItemStack[] targetItems = new ItemStack[INVENTORY_SIZE];

        // Заполняем рамку из серого стекла: верхний ряд, нижний ряд и боковые стороны
        ItemStack frameGlass = buildSideGlass(adapter);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                targetItems[i] = frameGlass;
            }
        }

        // Собираем список внутренних слотов (ряды 1..4, колонки 1..7, всего 28 слотов)
        List<Integer> contentSlots = new ArrayList<>(28);
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                contentSlots.add(row * 9 + col);
            }
        }

        List<String> homeNames = new ArrayList<>(homes.keySet());
        int allowedSlots = hasBypass ? contentSlots.size() : Math.min(contentSlots.size(), Math.max(homeLimit, homeNames.size()));
        ItemStack barrierItem = buildBarrierItem(homeLimit, lm, adapter);

        for (int i = 0; i < contentSlots.size(); i++) {
            int slot = contentSlots.get(i);

            if (i < homeNames.size()) {
                String name = homeNames.get(i);
                Home home = homes.get(name);
                if (home != null) {
                    targetItems[slot] = buildCompassItem(name, home, lm, adapter);
                    holder.setHomeAt(slot, name);
                }
            } else if (i < allowedSlots) {
                // В пределах лимита, но хом ещё не установлен — пустое место (null)
                targetItems[slot] = null;
            } else {
                // Превышает лимит хомов — барьер
                targetItems[slot] = barrierItem;
            }
        }

        return targetItems;
    }

    private static ItemStack buildSideGlass(PlatformAdapter adapter) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            adapter.setItemDisplayName(meta, " ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildBarrierItem(int limit, LanguageManager lm, PlatformAdapter adapter) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            adapter.setItemDisplayName(meta, lm.get("gui.barrier-name"));
            adapter.setItemLore(meta, lm.getStringList("gui.barrier-lore", "limit", limit));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildCompassItem(String name, Home home, LanguageManager lm, PlatformAdapter adapter) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            adapter.setItemDisplayName(meta, lm.get("gui.compass-name", "name", name));

            List<String> lore = new ArrayList<>();
            lore.add(lm.get("gui.compass-lore-world", "world", home.getWorld()));
            lore.add(lm.get("gui.compass-lore-coords",
                    "x", String.format(Locale.ROOT, "%.0f", home.getX()),
                    "y", String.format(Locale.ROOT, "%.0f", home.getY()),
                    "z", String.format(Locale.ROOT, "%.0f", home.getZ())));
            lore.add("");
            lore.add(lm.get("gui.compass-lore-teleport"));
            lore.add(lm.get("gui.compass-lore-manage"));
            adapter.setItemLore(meta, lore);

            item.setItemMeta(meta);
        }
        return item;
    }
}
