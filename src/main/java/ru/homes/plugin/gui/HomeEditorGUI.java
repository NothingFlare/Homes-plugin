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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;

import java.util.ArrayList;
import java.util.List;

public class HomeEditorGUI {

    public static final int INVENTORY_SIZE = 54;

    public static final int SLOT_TELEPORT = 20;
    public static final int SLOT_INFO = 22;
    public static final int SLOT_DELETE = 24;
    public static final int SLOT_BACK = 40;

    /**
     * Открывает редактор хома сразу со всеми предметами.
     */
    public static void open(Player player, HomesPlugin plugin, String homeName) {
        Inventory inv = build(player, plugin.getHomeManager(), homeName);
        if (inv != null) {
            player.openInventory(inv);
        }
    }

    public static Inventory build(Player player, HomeManager homeManager, String homeName) {
        Home home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home == null) return null;

        HomeEditorInventoryHolder holder = new HomeEditorInventoryHolder(homeName);
        Component title = Component.text(homeName, NamedTextColor.DARK_AQUA);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inv);

        ItemStack[] targetItems = buildTargetItems(homeName, home);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (targetItems[i] != null) {
                inv.setItem(i, targetItems[i]);
            }
        }

        return inv;
    }

    private static ItemStack[] buildTargetItems(String homeName, Home home) {
        ItemStack[] targetItems = new ItemStack[INVENTORY_SIZE];

        // Рамка из серого стекла со всех четырёх сторон
        ItemStack frameGlass = buildGlass();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                targetItems[i] = frameGlass;
            }
        }

        targetItems[SLOT_TELEPORT] = buildTeleportItem(homeName);
        targetItems[SLOT_INFO] = buildInfoItem(homeName, home);
        targetItems[SLOT_DELETE] = buildDeleteItem(homeName);
        targetItems[SLOT_BACK] = buildBackItem();

        return targetItems;
    }

    private static ItemStack buildGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildTeleportItem(String homeName) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Телепортироваться", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Нажмите для телепортации", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("к хому " + homeName + ".", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildInfoItem(String homeName, Home home) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Хом " + homeName, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Мир: ", NamedTextColor.GRAY)
                    .append(Component.text(home.getWorld(), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(String.format("X: %.1f  Y: %.1f  Z: %.1f",
                    home.getX(), home.getY(), home.getZ()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(String.format("Поворот: %.1f° / %.1f°",
                    home.getYaw(), home.getPitch()), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildDeleteItem(String homeName) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Подтвердить удаление", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Нажмите, чтобы безвозвратно", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("удалить хом " + homeName + ".", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Действие необратимо!", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Назад", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Вернуться к общему списку хомов", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }
}
